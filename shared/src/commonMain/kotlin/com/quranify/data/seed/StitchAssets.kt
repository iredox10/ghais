package com.quranify.data.seed

data class Reciter(
    val name: String,
    val fans: String,
    val photoUrl: String,
    val slug: String
)

data class JumpBackInItem(
    val title: String,
    val subtitle: String,
    val progress: Float,
    val coverUrl: String
)

data class CuratedPlaylist(
    val title: String,
    val tag: String,
    val coverUrl: String
)

object StitchAssets {
    val LogoUrl = "https://lh3.googleusercontent.com/aida/AEtjO1VSY3aPP_KJwBw8Z5b4fXNcpOg9Zslxkd6o3i7gPVIaruUyts9p6lNZewsVKJiK1RYWHf96fYxOm9PD4ojF1LGgA6cx-SlbkR6hRY-H6JeXh6k7RTCBDvVGH6qxu3cc9IpeI52x1d7HMcn6edFe6005jaVSsnmLZGqrvkdCacHCQ2KnupCjp2Lp5pvB0if1X3Tub7lcbfTSOHFEC__1EVSohZFe-S45_T6QSbELa71kWZHJJ0qIalmLxwrS"
    
    private const val AIDA_PUBLIC_PREFIX = "https://lh3.googleusercontent.com/aida-public/"
    
    val ProfileAvatarUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuDuBeyTCRrrvqY7hfjyOGnXicnlujK26o04oyiLhu7dGYUeLgSk8jJhlRCruN456ahgOBtlRMFh3u97OlbD7kTlazvNqBW-gWLgRNQ8HspRgL7yUZMMBwrhYS8F5hpjMjMdtiuPVFyeBKCH7B9N1_ZNsYKa0Ujo3QxuGHKYKh3Z6oJUySDGRakPnW-vLFPeW21zzHNwvhaGw556pLBDVoT_E3rUhccsbjjlzk7moe2u5oGz_Lmykdz0wg"

    val VerifiedReciters = listOf(
        Reciter(
            name = "Mishary",
            fans = "4.8M fans",
            photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuCVq7pqUhr_vS9hx1DOLeIcQ7oOf-d00g_cojjDLspmDlxOaQGBKbrZ372xlC7XtuhQTBfUHrTej_QIKrC1_RJzUrRLPQwbSae0rPEJ-PnW2FepBr_rO0F86S7MaqQrd8uELPi6_ip_6xuedtBym7Ag9yIZW_znPwtV706_u7vdSszFO0Go4EX6Fqw1-04de24SMZZW-f5PPH77Yk8BDtb2T2s2Fz5UKnd-ivOJLI1U66mNeJYMjGSgKQ",
            slug = "mishary"
        ),
        Reciter(
            name = "Al-Sudais",
            fans = "3.9M fans",
            photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuBUUXSSQnjbh9PcmTg5THpn3mvDpVVxiH_GKcHhZGP-taghc9BmoJoNWE0ft_q3y_Oeka9G4qJLbGRkde06mO-L_0Jv9oBcMgSlZe8g65QBl6b7lOIJnL6D5CybQ8PVppePQ8RipIaS7EpYks3LNyldifo5N6m6aXBJiDrAcUUJaZYNjuBZ4kax_YHflvxYG-C3CRtpzbSiSwsZ4W5mxnhkFPZFAFDQovfCGAWpPVUlyzOOpY1xBfc22Q",
            slug = "al-sudais"
        ),
        Reciter(
            name = "Al-Muaiqly",
            fans = "3.2M fans",
            photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuCe9KI2uHdihkC6MNA1pT-CAxgMZw9htwZaNdc9ZwyJcugtxNLhv2mjYhKAFgFh7VPapZ-UR3W9WT4nzRJRbmv4R7iEnluD_2_taI1U8TBOySJNo8M3P4Dr2Iz8QSsVy-5jwmzKmbkPiWfVOzWVZEPkL3ELv5JYOZlMKy6vLl3AThQbeuMk7Vt-b0WdUU-mJLdC-32gRmBq-a3sgm_hQGdLrwCnAMYkIWyGnBwSlodGMJgVTdMAEgkSfw",
            slug = "al-muaiqly"
        ),
        Reciter(
            name = "Al-Dossari",
            fans = "2.7M fans",
            photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuACIOW-S_FSJrlW0ZRGDNhL-x4HzJEmJIwIzuMKDReZE6rWbmX-muvOypYAECz9PA1sMWdEwIKEkjCtcMuCGbOusGgV0w59lNHUgrWz1ZaKQkgeTj4s6CSnVAdEp7TKuaCDrm_cmvqfK5vmKO6djuBmBdgkC5HOFFoB6Tzu_foAFiAmVi-SWqEOZK5NmjsEGM4YJI7YP8wAVvrwB958PTOCl3Uq3tTQYro_H30leRqVQ4W7NfRbZDGBLQ",
            slug = "al-dossari"
        )
    )

    val JumpBackInItems = listOf(
        JumpBackInItem(
            title = "Al-Mulk",
            subtitle = "Ayah 14 • 4:12 left",
            progress = 0.67f,
            coverUrl = "${AIDA_PUBLIC_PREFIX}cover_al_mulk"
        ),
        JumpBackInItem(
            title = "Al-Kahf",
            subtitle = "Friday Sunnah",
            progress = 0.25f,
            coverUrl = "${AIDA_PUBLIC_PREFIX}cover_al_kahf"
        ),
        JumpBackInItem(
            title = "Surah Yaseen",
            subtitle = "Mishary Alafasy",
            progress = 0.8f,
            coverUrl = "${AIDA_PUBLIC_PREFIX}cover_yaseen"
        ),
        JumpBackInItem(
            title = "Tahajjud Peace",
            subtitle = "Heart Softeners",
            progress = 0.5f,
            coverUrl = "${AIDA_PUBLIC_PREFIX}cover_tahajjud"
        )
    )

    val CuratedForPeace = listOf(
        CuratedPlaylist(
            title = "Deep Focus & Study",
            tag = "Tartil",
            coverUrl = "${AIDA_PUBLIC_PREFIX}cover_tartil"
        ),
        CuratedPlaylist(
            title = "Heart Soothing",
            tag = "Emotional",
            coverUrl = "${AIDA_PUBLIC_PREFIX}cover_emotional"
        ),
        CuratedPlaylist(
            title = "Morning Protection",
            tag = "Morning",
            coverUrl = "${AIDA_PUBLIC_PREFIX}cover_morning"
        )
    )
}
