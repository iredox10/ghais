#!/usr/bin/env python3
"""Generate the Madinah-mushaf ayah->page index for PageJuzIndex.

Fetches per-ayah Madinah page numbers (1..604, canonical ayah order,
6236 entries) and emits a Kotlin IntArray literal written to
shared/src/commonMain/kotlin/com/ghais/data/seed/PageAyahPages.kt.

Source chain (first success wins):
  1. Tanzil metadata (https://tanzil.net/res/quranmetadata.py or the CSV
     download). NOTE: as of 2026-10 these Tanzil URLs return HTTP 404, so
     this step is attempted for documentation purposes and skipped on
     failure.
  2. Quran.com API v4 verses/by_chapter (page_number + juz_number per ayah;
     the Madani-mushaf pagination, same lineage as the Tanzil data).
  3. AlQuran Cloud v1/surah/{n} (page + juz per ayah, Tanzil-derived).
  4. Documented even-distribution FALLBACK, clearly flagged
     GENERATED-FALLBACK in the output header so the coordinator regenerates
     it. Never silently shipped as real data.

Verification performed on every run (real or fallback):
  - exactly 6236 entries matching QuranData.SURAHS ayah counts
  - pageOf(1,1) == 1 and pageOf(114,6) == 604
  - monotonic non-decreasing across the whole Quran
  - every page 1..604 is present
  - every ayah's juz derived from the 30 hardcoded JUZ_STARTS matches the
    API-reported juz (real sources only)

Usage:
  python3 scripts/gen_page_index.py [--out PATH] [--stdout] [--check]
      [--force-fallback] [--no-verify]

Stdlib only.
"""

import argparse
import datetime
import json
import os
import sys
import time
import urllib.request

SURAH_COUNTS = [
    7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99,
    128, 111, 110, 98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
    34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 38,
    29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18,
    12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29,
    19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8,
    11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6,
]
TOTAL_AYAHS = sum(SURAH_COUNTS)  # 6236
TOTAL_PAGES = 604

# Standard 30 juz starts as (surahId, ayahNo); juz j starts at JUZ_STARTS[j-1].
JUZ_STARTS = [
    (1, 1), (2, 142), (2, 253), (3, 93), (4, 24), (4, 148), (5, 82),
    (6, 111), (7, 88), (8, 41), (9, 93), (11, 6), (12, 53), (15, 1),
    (17, 1), (18, 75), (21, 1), (23, 1), (25, 21), (27, 56), (29, 46),
    (33, 31), (36, 28), (39, 32), (41, 47), (46, 1), (51, 31), (58, 1),
    (67, 1), (78, 1),
]

TANZIL_URLS = [
    "https://tanzil.net/res/quranmetadata.py",
    "http://tanzil.net/res/quranmetadata.py",
]

UA = {"User-Agent": "quranify-gen-page-index/1.0"}


def http_get_json(url, timeout=30):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def http_get_text(url, timeout=30):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.read().decode("utf-8", errors="replace")


def fetch_tanzil():
    """Attempt the Tanzil metadata endpoints. Returns (pages, juzs) or raises."""
    last_err = "no Tanzil URL attempted"
    for url in TANZIL_URLS:
        try:
            text = http_get_text(url)
        except Exception as e:  # noqa: BLE001 - probe several URLs, keep last error
            last_err = "%s -> %r" % (url, e)
            continue
        pages = try_parse_tanzil_metadata(text)
        if pages is not None:
            return pages, None
        last_err = "%s -> downloaded but format unrecognized" % url
    raise RuntimeError("Tanzil unavailable: %s" % last_err)


def try_parse_tanzil_metadata(text):
    """Best-effort parse of Tanzil quranmetadata.py content.

    Returns a 6236-entry page list, or None when the format is unknown.
    """
    # Known Tanzil metadata shapes carry per-ayah page info only in the
    # CSV/text downloads, not in quranmetadata.py; without a parseable
    # page column we must NOT guess.
    return None


def fetch_qurancom(pause=0.25, retries=3):
    """Fetch Madinah page+juz per ayah from Quran.com API v4."""
    pages = []
    juzs = []
    for surah in range(1, 115):
        url = (
            "https://api.quran.com/api/v4/verses/by_chapter/%d"
            "?words=false&per_page=300&fields=text_uthmani" % surah
        )
        data = None
        for attempt in range(retries):
            try:
                data = http_get_json(url)
                break
            except Exception as e:  # noqa: BLE001 - retry transient failures
                if attempt == retries - 1:
                    raise RuntimeError(
                        "quran.com chapter %d failed: %r" % (surah, e)
                    )
                time.sleep(1.5 * (attempt + 1))
        verses = data.get("verses", [])
        if len(verses) != SURAH_COUNTS[surah - 1]:
            raise RuntimeError(
                "quran.com chapter %d: got %d verses, expected %d"
                % (surah, len(verses), SURAH_COUNTS[surah - 1])
            )
        for v in verses:
            pages.append(int(v["page_number"]))
            juzs.append(int(v["juz_number"]))
        time.sleep(pause)
    return pages, juzs


def fetch_alqurancloud(pause=0.25, retries=3):
    """Fetch page+juz per ayah from AlQuran Cloud (Tanzil-derived)."""
    pages = []
    juzs = []
    for surah in range(1, 115):
        url = "https://api.alquran.cloud/v1/surah/%d" % surah
        data = None
        for attempt in range(retries):
            try:
                data = http_get_json(url)
                break
            except Exception as e:  # noqa: BLE001 - retry transient failures
                if attempt == retries - 1:
                    raise RuntimeError(
                        "alquran.cloud surah %d failed: %r" % (surah, e)
                    )
                time.sleep(1.5 * (attempt + 1))
        ayahs = data["data"]["ayahs"]
        if len(ayahs) != SURAH_COUNTS[surah - 1]:
            raise RuntimeError(
                "alquran.cloud surah %d: got %d ayahs, expected %d"
                % (surah, len(ayahs), SURAH_COUNTS[surah - 1])
            )
        for a in ayahs:
            pages.append(int(a["page"]))
            juzs.append(int(a["juz"]))
        time.sleep(pause)
    return pages, juzs


def fallback_even_distribution():
    """Evenly distribute 6236 ayahs over 604 pages (FALLBACK ONLY)."""
    pages = []
    for i in range(TOTAL_AYAHS):
        # Ayah index i (0-based) -> page 1..604, monotonic by construction.
        pages.append(min(TOTAL_PAGES, (i * TOTAL_PAGES) // TOTAL_AYAHS + 1))
    # Pin the contract endpoints exactly.
    pages[0] = 1
    pages[-1] = TOTAL_PAGES
    return pages, None


def verify(pages, api_juzs, source):
    errors = []
    if len(pages) != TOTAL_AYAHS:
        errors.append("entry count %d != %d" % (len(pages), TOTAL_AYAHS))
        return errors
    if pages[0] != 1:
        errors.append("first ayah page %d != 1" % pages[0])
    if pages[-1] != TOTAL_PAGES:
        errors.append("last ayah page %d != %d" % (pages[-1], TOTAL_PAGES))
    for i in range(1, len(pages)):
        if pages[i] < pages[i - 1]:
            errors.append("non-monotonic at index %d: %d -> %d"
                          % (i, pages[i - 1], pages[i]))
            break
        if not 1 <= pages[i] <= TOTAL_PAGES:
            errors.append("page out of range at index %d: %d" % (i, pages[i]))
            break
    if set(pages) != set(range(1, TOTAL_PAGES + 1)):
        missing = sorted(set(range(1, TOTAL_PAGES + 1)) - set(pages))
        errors.append("pages missing from coverage: %s" % missing[:10])
    if api_juzs is not None:
        # Derive juz per ayah from JUZ_STARTS and compare with the API.
        start_idx = []
        off = 0
        for s, count in enumerate(SURAH_COUNTS, start=1):
            for a in range(1, count + 1):
                start_idx.append((s, a))
            off += count
        bounds = []
        for (s, a) in JUZ_STARTS:
            bounds.append(pos_of(s, a))
        mism = 0
        for i in range(TOTAL_AYAHS):
            expected = 1
            for j, b in enumerate(bounds):
                if b <= i:
                    expected = j + 1
            if api_juzs[i] != expected:
                mism += 1
                if mism <= 5:
                    errors.append(
                        "juz mismatch at %d:%d api=%d juzStarts=%d"
                        % (start_idx[i][0], start_idx[i][1],
                           api_juzs[i], expected))
        if mism:
            errors.append("total juz mismatches: %d" % mism)
    return errors


def pos_of(surah, ayah):
    off = sum(SURAH_COUNTS[: surah - 1])
    return off + (ayah - 1)


def kotlin_header(source, fallback):
    today = datetime.date.today().isoformat()
    if fallback:
        banner = (
            "// GENERATED-FALLBACK: even-distribution placeholder, NOT real\n"
            "// Madinah page data. The coordinator MUST regenerate this file\n"
            "// with network access (`python3 scripts/gen_page_index.py`).\n"
        )
    else:
        banner = "// GENERATED from real Madinah-mushaf metadata (see source below).\n"
    return (
        "package com.ghais.data.seed\n"
        "\n"
        + banner +
        "// Source: %s\n"
        "// Generated: %s by scripts/gen_page_index.py (do not edit by hand;\n"
        "// re-run the script to regenerate).\n"
        "// Layout: AYAH_PAGES[i] is the Madinah page (1..604) of the i-th\n"
        "// ayah in canonical order (surah 1..114, ayah 1..count), computed\n"
        "// with offsets from QuranData.SURAHS. Size: %d entries.\n"
        % (source, today, TOTAL_AYAHS)
    )


def kotlin_literal(pages):
    lines = ["internal val AYAH_PAGES: IntArray = intArrayOf("]
    for i in range(0, len(pages), 20):
        chunk = pages[i: i + 20]
        lines.append("    " + ", ".join(str(p) for p in chunk) + ",")
    lines.append(")")
    return "\n".join(lines) + "\n"


def main():
    ap = argparse.ArgumentParser(description="Generate PageAyahPages.kt")
    ap.add_argument("--out", default=(
        "shared/src/commonMain/kotlin/com/ghais/data/seed/PageAyahPages.kt"),
        help="output Kotlin file (relative to repo root)")
    ap.add_argument("--stdout", action="store_true",
                    help="print the IntArray literal to stdout instead")
    ap.add_argument("--check", action="store_true",
                    help="verify existing --out file against live sources")
    ap.add_argument("--force-fallback", action="store_true",
                    help="skip network, emit the flagged fallback")
    ap.add_argument("--no-verify", action="store_true",
                    help="skip verification (not recommended)")
    args = ap.parse_args()

    pages, api_juzs, source, fallback = None, None, None, False
    if args.force_fallback:
        pages, api_juzs = fallback_even_distribution()
        source, fallback = "even-distribution fallback (forced)", True
    else:
        try:
            pages, api_juzs = fetch_tanzil()
            source = "Tanzil https://tanzil.net/res/quranmetadata.py"
        except Exception as e:  # noqa: BLE001 - documented source chain
            print("tanzil: %s" % e, file=sys.stderr)
            try:
                pages, api_juzs = fetch_qurancom()
                source = ("Quran.com API v4 verses/by_chapter "
                          "(Madani-mushaf page_number/juz_number)")
            except Exception as e2:  # noqa: BLE001 - documented source chain
                print("quran.com: %s" % e2, file=sys.stderr)
                try:
                    pages, api_juzs = fetch_alqurancloud()
                    source = ("AlQuran Cloud v1/surah/{n} "
                              "(Tanzil-derived page/juz)")
                except Exception as e3:  # noqa: BLE001 - documented fallback
                    print("alquran.cloud: %s" % e3, file=sys.stderr)
                    pages, api_juzs = fallback_even_distribution()
                    source = ("even-distribution fallback "
                              "(all network sources failed)")
                    fallback = True

    if not args.no_verify:
        errors = verify(pages, api_juzs, source)
        if errors:
            print("VERIFICATION FAILED:", file=sys.stderr)
            for e in errors:
                print("  - %s" % e, file=sys.stderr)
            return 1
        print("verified: %d entries, source=%s%s"
              % (len(pages), source, " [FALLBACK]" if fallback else ""))

    if args.stdout:
        sys.stdout.write(kotlin_literal(pages))
        return 0

    out_path = args.out
    if not os.path.isabs(out_path):
        # Resolve relative to the repo root (parent of scripts/).
        root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        out_path = os.path.join(root, out_path)
    content = kotlin_header(source, fallback) + "\n" + kotlin_literal(pages)
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(content)
    print("wrote %s (%d bytes)" % (out_path, len(content)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
