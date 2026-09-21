package com.ghais.data.seed

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
    val coverUrl: String,
    val surahId: Int = 67,
    val reciterSlug: String = "mishary",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastPlayedTimestampMs: Long = 0L
)

data class CuratedPlaylist(
    val title: String,
    val tag: String,
    val subtitle: String = "",
    val coverUrl: String,
    val id: String = title.lowercase().replace(" ", "-").replace("&", "and").replace("(", "").replace(")", "").replace("'", ""),
    val trackCount: Int = 8,
    val durationText: String = "35 mins",
    val category: String = "Peace"
)

data class LibraryPlaylistItem(
    val title: String,
    val subtitle: String,
    val category: String,
    val coverUrl: String
)

object GhaisAssets {
    // Local Android / Compose Drawable Resource Names
    const val DrawableLogoName = "ghais_logo"
    const val DrawableAvatarName = "user_avatar"

    val LogoUrl = "https://lh3.googleusercontent.com/aida/AEtjO1VSY3aPP_KJwBw8Z5b4fXNcpOg9Zslxkd6o3i7gPVIaruUyts9p6lNZewsVKJiK1RYWHf96fYxOm9PD4ojF1LGgA6cx-SlbkR6hRY-H6JeXh6k7RTCBDvVGH6qxu3cc9IpeI52x1d7HMcn6edFe6005jaVSsnmLZGqrvkdCacHCQ2KnupCjp2Lp5pvB0if1X3Tub7lcbfTSOHFEC__1EVSohZFe-S45_T6QSbELa71kWZHJJ0qIalmLxwrS"
    
    private const val AIDA_PUBLIC_PREFIX = "https://lh3.googleusercontent.com/aida-public/"
    
    val ProfileAvatarUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuDuBeyTCRrrvqY7hfjyOGnXicnlujK26o04oyiLhu7dGYUeLgSk8jJhlRCruN456ahgOBtlRMFh3u97OlbD7kTlazvNqBW-gWLgRNQ8HspRgL7yUZMMBwrhYS8F5hpjMjMdtiuPVFyeBKCH7B9N1_ZNsYKa0Ujo3QxuGHKYKh3Z6oJUySDGRakPnW-vLFPeW21zzHNwvhaGw556pLBDVoT_E3rUhccsbjjlzk7moe2u5oGz_Lmykdz0wg"
    val ProfileAvatarGlassmorphicUrl = "https://lh3.googleusercontent.com/aida/AEtjO1UCF5PmEzAuhY7h-DkvBOR6w6id1nLrU6RUzlsj3RtRRCuNHGbnvzSxg7yx7cBmZeMcPGYAo_FVvoNHzvAozB2URyU4ah9pshKza7ZJUnhrwfOj7YG0qpfg7016k1YsXyF59VejcFIUuII1qodt1lvGBl1GD8p7gDUMX3Va-GcixMHqkX9vJr5KPIpLxSd87Yj7HBU32_fLSCh4O3OXu82KPfN1cJWSmAv8ATMoqRX6BLQsBd4p6vhf8O4S"

    val NowPlayingVinylArtUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuALlpMBPBacxkPRdcMkbu1y7Bqa6-QgMU0XhdsI4YhU-Pf1GxauCukcZWbgJ_1LnzuT0DkihtJcEkXwqYUrThfl8OSeAwn8DIYTbA7jtvBp86ExyvUybWuLTovpulH5u6XoRd3E8yibUJjHD-Sb-8sT0jUM592Au7jOKDWRWpMIkHu8Y7DXfix-G_P_a4zSuh7hcK0RI-Ca8OuepAg07GUG9q2Ziv6ylcLhrxBmfhQWKy0zYxRZUvzTTA"
    val NowPlayingArtworkUrl = NowPlayingVinylArtUrl

    val LibraryTahajjudCover = "${AIDA_PUBLIC_PREFIX}AB6AXuD9gN73HhJ1ixxzQ5VYaXQW0x3jai4zjChJwGIbmYWKKaCFI9mPOeKfHmRom2I26MKNWOoFK1Eiz7Ouqq4DrlvWl2Mqh68nPRus3BKWncVsLH5qSRutACjuv3xmGfrGB5Ib1wpc_OzbHMx5qjz0OnoMrcgmkEHfspzQvAQX9QygQXTw83nPCgEKKJctuoSTxb-n00gl88250D7eVlOlyAXVVS26zs_tlFaApGNsm7wv9iFy80YmdMUDMA"
    val LibraryMorningCover = "${AIDA_PUBLIC_PREFIX}AB6AXuAOrzq8xVyzPbYBeS771NhDV7-l2tN_P7FFiedZ0tr_Japu-WaUSVhG32AqzXjw2HaD1PJm-auCagCW8w2lljyklMwvpVrHSnbQ2RKRVpMIYM7ku_qSl8RUnPO_YbVLxdkJtD_1osCFoqsPeF6sTJ4eL9bvcwtEQOcsUBt_cpiScpZKz_Yjeeac53vgzCqLx5SmLMQAF8h__vNX3jq2NS5mXBGk_EKqfzKd91hhm8OP_Q7B5XqnuRrG5g"
    val LibraryMisharyAvatar = "${AIDA_PUBLIC_PREFIX}AB6AXuA7ppBI-mBpDcMXZBQBLHr3cDTqVc0AUVJ1NJnwqguMxpA0hBzodlv2AaPxNbG-l-ebm1QYLeeBXgFrS-8mrWVkcKdVlIKvO0bCKSt073yPcQR76sTbT3wxoHkKtDy0c0ME9Ou7WCDSNKjcv6FDci6c_JCN8BMnzHc36UNKKfQYdfgK2HDpTNtdT1rSvv22AyjRX-Cm_lOXiOU7Af_MrE1Gp2ehp4QMy_ErC1iY2L1SB2BFyDYYbLLYbw"

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
        ),
        Reciter(
            name = "Islam Sobhi",
            fans = "2.1M fans",
            photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuCzOyQUcWQrLRalTR2UVt5Z61FZm1cBuurJEVrVvWaTHkjM_wmWbYuMiVPbFQQdy4nSbVo5uZRGlb1d4Tt0g5e5fTo2Ou2ZJextr0N1pLm9wyVpRTSD8kp35B-W9UQZIt985fydj0nseJuSmjOmpAkRgnbfVQQohu43Z1CW3EAn7amGbcZD7tlweCQ-JBaZhBJDvCmWu9rKzcD-qIWhGvGxKomXe4KBSWNdQso65BWAnXNujB3s3BaPiw",
            slug = "islam-sobhi"
        ),
        Reciter(
            name = "Omar Hisham",
            fans = "1.5M fans",
            photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuDgsbtNwrTvZMDxhet9KzTJPINfQqnQ2Gc74H1NotuIf_y1ZYbY3ChOWF2Rvly3n97R741_TRWgpd9CXEZUIhOCQJMCUWrHGCDLra3ozOJUdRllZlWJVjFumo6IU0Nn21IafKwYHzi0oGVNgAiqQ1feu1y0UAX2l0_E-k108XS4KFJbXV7nSETDDmeqfDUHp5Kp-KE3necEvLlvnSTmqvOEUjLwtC3P4ta3bURWLF70RzPfdAx2C1nDGw",
            slug = "omar-hisham"
        ),
        Reciter(
            name = "Abdul Basit",
            fans = "5.1M fans",
            photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuCDC0-4ROINJxYGSxbAWcYctH_0THHCxU5gubgORkVyhQMyl9AvpNpePXHQJdS67zKw8eMaouJ-zk1C5sAyIDHw4NqZxAP6m5LDQVhG2HBSP9sX2UjwEqyuvH-RACVPK_MlMFly-G9PwG0l8DyZPoP-iht8qz4j0_OxBwXdCpefdRao2p1fhn4_Rjwa8jkWOlDMtNhOBHVpCb-RHhRTDhvMJWsg81tFxI5rc8CMpg2ZyKYJLvJfvv7ZAQ",
            slug = "abdul-basit"
        )
    )

    val JumpBackInItems = listOf(
        JumpBackInItem(
            title = "Al-Mulk",
            subtitle = "Ayah 14 • 4:12 left",
            progress = 0.67f,
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuCrHh8KdAdFyI3sO2ZmLEaF1tEHrUkskxWBu07rF_q8JyrKScuJYBDyQfjzeXgIGAHCal9DfF_EmMzhvauFJR6uLxwbjT5A5RhA4Z21DDguAkVAjVl5wkEPZ3pf2GNbg0UgjkeoMiMb2Rw6VYj66WaJNknVnChJOsytdnYXdUcrBQPsRsY9MWde5lkfLLbsjX7Cj_cTvyzye_ViP53Z6FPfa9MbqWUleiUuZYpuMo9HJE7QhefD2F1euA",
            surahId = 67,
            reciterSlug = "mishary"
        ),
        JumpBackInItem(
            title = "Al-Kahf",
            subtitle = "Friday Sunnah",
            progress = 0.25f,
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuDfvEfM9eGeta57gmoKvCrVxuZ3oSNhWhSX2bU93QlbAGiavblVoxP_iY2CyTDgTO1nf_hRJVu1dP9BWtX37bZMe4sFlRqE-1rTP1ATpVeQuAepkC4r-NeKWsGq9Zq0UUJXLoG7QCOBY7W-0id_bid5AkOpBkIC49S5PCC2eg6TKa-HRKhpgVvN-NW8N9DTgnb72PTOAbXbBhriSMmIE_YcAg81INi-TaMYD_T_plEieFW1BcV_ZnGLyQ",
            surahId = 18,
            reciterSlug = "mishary"
        ),
        JumpBackInItem(
            title = "Surah Yaseen",
            subtitle = "Mishary Alafasy",
            progress = 0.8f,
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuBUe24go4tJzvwNZUQ_8UwI6gEDDgTs_Ol7UaxfpYa06PeqOU0r5u-XwOTVNmQjzyq_972QXKLDJoK59z0UZ0qxUrflzOtvpFjxj6xmO5Q6GknBk6q1q_Qs_siEE3_zB7EMQvVVrTvH3-fVefTA5zNQyAjH_rl0X6jr0lYyy3CCRjyGpCexQtAycX3UYvWTJWqnnyau-m9c0aDyJVVyf69-DywZIXP7sFIyDPgIKytzrnbX-KaWQP5fOQ",
            surahId = 36,
            reciterSlug = "mishary"
        ),
        JumpBackInItem(
            title = "Tahajjud Peace",
            subtitle = "Heart Softeners",
            progress = 0.5f,
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuAPr6kdXhmVnAhFpZ9oCk9dJWVv4v2a0b7jMNoRAiFzDL3Zb2JTlEZFPWGrsHoJvC_j9LHL6o3LHjOGEmZxD-Kcr5BtvYyVn9yAGX-QwG7xwR4l8FCfBK1HREyPEI3cmK1w1EiYFQ1W1CCcPgEFCx0Kg0vYKDNoCvnwQ0_JI2Bkj_Efej3keX09Arqn1LyetzADwjOE4hHWxItZKSjV-8SRVESwxRwZZnLmvtjUCjN8QE4N--QjvqGpEQ",
            surahId = 55,
            reciterSlug = "mishary"
        )
    )

    val CuratedForPeace = listOf(
        CuratedPlaylist(
            title = "Deep Focus & Study",
            tag = "Tartil",
            subtitle = "Calm, slow tempo recitation",
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuAvJyeKgVjglzfWobcAH4Ng4YYbpbs_2fS1IWOSu1Fz-oQIhOMNH1ZzeVNknZCQDdjVSugK4iQ4UjFSNvaxH38zOWhM03xyJgvDPQyiZQyLaNRss6725PFTvsitvHiLlUfXXXDqHM8CwmgsYwD_Vnvia0rdeAb2U8Gq6Pt9hYdHkCGVCZ82NG7ACYdw_hjO8-duwxdsY1OLHmt8oTUexN6cj5hJPbXJwIeEyH7JbMLruDst6iZdffrsjA"
        ),
        CuratedPlaylist(
            title = "Heart Soothing",
            tag = "Emotional",
            subtitle = "Comforting verses of mercy",
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuDC7Seq-quBq_0CKLw3H6BEAermI0knNV2qvaJ7wCAIav9Ahp7jkTyyQVF5y-fB6ufMglhzL-1pA4z_qQMS7ghQBCHMA86n8SHAo88kPQbHLl_mQIWDpUkzmioA2EKoMwsaAP10CKJKTP4SdwSboWHQtpXe9XgzmQwbgNWCd_mmVUN7veSZJNIa0kI6U7WDF516jtVkk5In-Pgh4QjmtJjAVnIi9dArMmoxa-4nkIo3hTvTukt2_F4tLQ"
        ),
        CuratedPlaylist(
            title = "Morning Adhkar",
            tag = "Morning",
            subtitle = "Protection & Barakah",
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuAmXfd4POG8k4TnIAguN0g7KfPFns5LeXPt9CuUdQFsrrJ7r671OZWhFRVhFsjzt5WS49rwcpXt5vKVePd221RFYoNezS0XxYACLqdKyHaVI_NZlrRdH-qcrQxDzuiQ0dXVw2ghZylgzFXPA3cXU2cWwY4lqHT9bWpEs8iR7_o-OK0Y1-P6s4slstQm01kyRDGVJvRmYaJeV-PvtHvQR15zkE6R36IzcBGu5VP2QzlMEaQYGrSom8hMlQ"
        ),
        CuratedPlaylist(
            title = "Bedtime Sakinah",
            tag = "Sleep",
            subtitle = "Gentle sleep timer mix",
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuCkC8No81KMkOzXz1WgSpwHPzqud63axS2WjPtLIC6_W7F3Wd9gX3465Bbu4pZr9e7krossjqUUaYsHrd9CB9HstJhcsKtxkawDEc5F90DGCjWfhxHxblmFCxPXMP4unstKiSAvhEk1OoKPmMh9ehfsTJoVz6Oc9yGFTs1lfgrRODGKmeh3ycxH1qVyhs2XEJ0-ZOcmiCtsASjF74TQ_dvUwSVO7AEU9yR-_CE-SdRGlIQKc13_BnfmjA"
        )
    )

    val AllCuratedPlaylists = listOf(
        CuratedPlaylist(
            title = "Deep Focus & Study",
            tag = "Focus",
            subtitle = "Calm, slow tempo recitation",
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuAvJyeKgVjglzfWobcAH4Ng4YYbpbs_2fS1IWOSu1Fz-oQIhOMNH1ZzeVNknZCQDdjVSugK4iQ4UjFSNvaxH38zOWhM03xyJgvDPQyiZQyLaNRss6725PFTvsitvHiLlUfXXXDqHM8CwmgsYwD_Vnvia0rdeAb2U8Gq6Pt9hYdHkCGVCZ82NG7ACYdw_hjO8-duwxdsY1OLHmt8oTUexN6cj5hJPbXJwIeEyH7JbMLruDst6iZdffrsjA",
            id = "deep-focus-study",
            trackCount = 8,
            durationText = "42 mins",
            category = "Focus"
        ),
        CuratedPlaylist(
            title = "Heart Soothing & Mercy",
            tag = "Peace",
            subtitle = "Comforting verses of divine tranquility",
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuDC7Seq-quBq_0CKLw3H6BEAermI0knNV2qvaJ7wCAIav9Ahp7jkTyyQVF5y-fB6ufMglhzL-1pA4z_qQMS7ghQBCHMA86n8SHAo88kPQbHLl_mQIWDpUkzmioA2EKoMwsaAP10CKJKTP4SdwSboWHQtpXe9XgzmQwbgNWCd_mmVUN7veSZJNIa0kI6U7WDF516jtVkk5In-Pgh4QjmtJjAVnIi9dArMmoxa-4nkIo3hTvTukt2_F4tLQ",
            id = "heart-soothing",
            trackCount = 10,
            durationText = "48 mins",
            category = "Peace"
        ),
        CuratedPlaylist(
            title = "Morning Adhkar & Barakah",
            tag = "Morning",
            subtitle = "Protection & Barakah for dawn",
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuAmXfd4POG8k4TnIAguN0g7KfPFns5LeXPt9CuUdQFsrrJ7r671OZWhFRVhFsjzt5WS49rwcpXt5vKVePd221RFYoNezS0XxYACLqdKyHaVI_NZlrRdH-qcrQxDzuiQ0dXVw2ghZylgzFXPA3cXU2cWwY4lqHT9bWpEs8iR7_o-OK0Y1-P6s4slstQm01kyRDGVJvRmYaJeV-PvtHvQR15zkE6R36IzcBGu5VP2QzlMEaQYGrSom8hMlQ",
            id = "morning-adhkar",
            trackCount = 6,
            durationText = "28 mins",
            category = "Morning"
        ),
        CuratedPlaylist(
            title = "Bedtime Sakinah & Sleep",
            tag = "Night",
            subtitle = "Gentle sleep timer mix with soothing cadence",
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuCkC8No81KMkOzXz1WgSpwHPzqud63axS2WjPtLIC6_W7F3Wd9gX3465Bbu4pZr9e7krossjqUUaYsHrd9CB9HstJhcsKtxkawDEc5F90DGCjWfhxHxblmFCxPXMP4unstKiSAvhEk1OoKPmMh9ehfsTJoVz6Oc9yGFTs1lfgrRODGKmeh3ycxH1qVyhs2XEJ0-ZOcmiCtsASjF74TQ_dvUwSVO7AEU9yR-_CE-SdRGlIQKc13_BnfmjA",
            id = "bedtime-sakinah",
            trackCount = 12,
            durationText = "55 mins",
            category = "Night"
        ),
        CuratedPlaylist(
            title = "Ayat Ash-Shifa & Healing",
            tag = "Healing",
            subtitle = "Sacred verses of spiritual restoration",
            coverUrl = LibraryTahajjudCover,
            id = "ayat-ash-shifa",
            trackCount = 7,
            durationText = "36 mins",
            category = "Healing"
        ),
        CuratedPlaylist(
            title = "Mindful Memorization (Hifz)",
            tag = "Focus",
            subtitle = "Rhythmic murattal loops for deep retention",
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuBUe24go4tJzvwNZUQ_8UwI6gEDDgTs_Ol7UaxfpYa06PeqOU0r5u-XwOTVNmQjzyq_972QXKLDJoK59z0UZ0qxUrflzOtvpFjxj6xmO5Q6GknBk6q1q_Qs_siEE3_zB7EMQvVVrTvH3-fVefTA5zNQyAjH_rl0X6jr0lYyy3CCRjyGpCexQtAycX3UYvWTJWqnnyau-m9c0aDyJVVyf69-DywZIXP7sFIyDPgIKytzrnbX-KaWQP5fOQ",
            id = "mindful-hifz",
            trackCount = 14,
            durationText = "1h 10m",
            category = "Focus"
        ),
        CuratedPlaylist(
            title = "Tahajjud & Night Qiyam",
            tag = "Night",
            subtitle = "Deep emotional recitations in stillness",
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuAPr6kdXhmVnAhFpZ9oCk9dJWVv4v2a0b7jMNoRAiFzDL3Zb2JTlEZFPWGrsHoJvC_j9LHL6o3LHjOGEmZxD-Kcr5BtvYyVn9yAGX-QwG7xwR4l8FCfBK1HREyPEI3cmK1w1EiYFQ1W1CCcPgEFCx0Kg0vYKDNoCvnwQ0_JI2Bkj_Efej3keX09Arqn1LyetzADwjOE4hHWxItZKSjV-8SRVESwxRwZZnLmvtjUCjN8QE4N--QjvqGpEQ",
            id = "tahajjud-night-qiyam",
            trackCount = 9,
            durationText = "50 mins",
            category = "Night"
        ),
        CuratedPlaylist(
            title = "Anxiety Relief & Inshirah",
            tag = "Healing",
            subtitle = "Ash-Sharh & Ad-Duha for weary souls",
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuDfvEfM9eGeta57gmoKvCrVxuZ3oSNhWhSX2bU93QlbAGiavblVoxP_iY2CyTDgTO1nf_hRJVu1dP9BWtX37bZMe4sFlRqE-1rTP1ATpVeQuAepkC4r-NeKWsGq9Zq0UUJXLoG7QCOBY7W-0id_bid5AkOpBkIC49S5PCC2eg6TKa-HRKhpgVvN-NW8N9DTgnb72PTOAbXbBhriSMmIE_YcAg81INi-TaMYD_T_plEieFW1BcV_ZnGLyQ",
            id = "anxiety-relief-inshirah",
            trackCount = 8,
            durationText = "32 mins",
            category = "Healing"
        ),
        CuratedPlaylist(
            title = "Sunrise Barakah & Gratitude",
            tag = "Morning",
            subtitle = "Surah Ar-Rahman & Al-Waqi'ah recitations",
            coverUrl = LibraryMorningCover,
            id = "sunrise-barakah",
            trackCount = 8,
            durationText = "40 mins",
            category = "Morning"
        ),
        CuratedPlaylist(
            title = "Garden of Tranquility",
            tag = "Peace",
            subtitle = "Soft acoustic ambiance with Surat Maryam",
            coverUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuCrHh8KdAdFyI3sO2ZmLEaF1tEHrUkskxWBu07rF_q8JyrKScuJYBDyQfjzeXgIGAHCal9DfF_EmMzhvauFJR6uLxwbjT5A5RhA4Z21DDguAkVAjVl5wkEPZ3pf2GNbg0UgjkeoMiMb2Rw6VYj66WaJNknVnChJOsytdnYXdUcrBQPsRsY9MWde5lkfLLbsjX7Cj_cTvyzye_ViP53Z6FPfa9MbqWUleiUuZYpuMo9HJE7QhefD2F1euA",
            id = "garden-of-tranquility",
            trackCount = 11,
            durationText = "46 mins",
            category = "Peace"
        )
    )

    val LibraryPlaylists = listOf(
        LibraryPlaylistItem(
            title = "Tahajjud & Night Qiyam",
            subtitle = "18 recitations • Updated yesterday",
            category = "playlists",
            coverUrl = LibraryTahajjudCover
        ),
        LibraryPlaylistItem(
            title = "Morning Adhkar & Protection",
            subtitle = "12 tracks • Downloaded",
            category = "playlists downloads",
            coverUrl = LibraryMorningCover
        ),
        LibraryPlaylistItem(
            title = "Mishary Rashid Alafasy",
            subtitle = "114 Surahs • Reciter",
            category = "reciters",
            coverUrl = LibraryMisharyAvatar
        )
    )
}

