package com.example.data

import com.example.model.RahmaReel
import com.example.model.ReelComment
import com.example.model.Song
import com.example.model.SongCategory

object RahmaCatalog {
    val songs: List<Song> = listOf(
        Song(
            id = "alkawkab",
            title = "Al Kawkab",
            arabicTitle = "الكوكب",
            category = SongCategory.CLIPS,
            durationSeconds = 248,
            durationFormatted = "4:08",
            viewsCount = "231,840,290 مشاهدة",
            releaseYear = "2021",
            description = "الأغنية الأيقونية التي حققت نجاحاً عربياً وعالمياً كاسحاً وتصدرت التريند لأشهر طويلة بمئات ملايين المشاهدات.",
            lyrics = """كون نصير اني وياك
نجمة بالسما وغيمة
نسافر فوك ما نرجع
نعوف الكوكب وضيمه

كون نصير اني وياك
نجمة بالسما وغيمة
نسافر فوك ما نرجع
نعوف الكوكب وضيمه

وحدنا نعيش ع المريخ
حتى فد هدوء نلكى
يا حبيبي الهوى العشاك
بالصدفة اذا نلگه

احبك حب ولا صاير
عليك اتخبل واغار
يمك انسى كل همي
وبحبك عقلي طار""",
            composer = "علي صابر",
            lyricist = "ماهر العزاوي",
            isNewRelease = false,
            gradientStartHex = 0xFF990000,
            gradientEndHex = 0xFF1C0A00,
            youtubeVideoId = "VlhKzTzVfqg",
            likesCount = "1.9M إعجاب",
            commentsCount = "98.4K تعليق",
            channelSubscribers = "5.8M مشترك",
            publishDate = "18 أغسطس 2021",
            trendingRank = "#1 في أفضل الأغاني المصورة على يوتيوب"
        ),
        Song(
            id = "waed_menni",
            title = "Waed Menni",
            arabicTitle = "وعد مني",
            category = SongCategory.CLIPS,
            durationSeconds = 265,
            durationFormatted = "4:25",
            viewsCount = "216,420,110 مشاهدة",
            releaseYear = "2018",
            description = "أولى الأغاني العراقية النسائية التي حطمت أرقام المشاهدات القياسية على منصة يوتيوب وتعتبر علامة فارقة في تاريخ الفن العراقي.",
            lyrics = """اريد الله يسامحني
لان اذيت نفسي هواي
عفت الماي والزاد
وركضت ورا السراب اللي ما ينطاق

وعد مني بعد ما انطي
قلبي لأي بشر يسوى وما يسوى
حبيبي الما قدر تعبي
بعد ماريده لو يبقى بس هو

يا قلبي كافي طيبة زايدة
ترا العالم قست ما بيهم فايدة
اليوفي بهاي الدنيا خسران
واني من الوفا تعبان وندمان""",
            composer = "علي صابر",
            lyricist = "رامي العبودي",
            isNewRelease = false,
            gradientStartHex = 0xFF7A001E,
            gradientEndHex = 0xFF140509,
            youtubeVideoId = "jD1JqL0K2tM",
            likesCount = "1.7M إعجاب",
            commentsCount = "84.2K تعليق",
            channelSubscribers = "5.8M مشترك",
            publishDate = "28 أغسطس 2018",
            trendingRank = "الأكثر استماعاً في تاريخ الأغنية العراقية"
        ),
        Song(
            id = "asaad_lel_gomar",
            title = "As'ad Lel Gomar",
            arabicTitle = "أصعد للكمر",
            category = SongCategory.LATEST,
            durationSeconds = 215,
            durationFormatted = "3:35",
            viewsCount = "114,800,500 مشاهدة",
            releaseYear = "2022",
            description = "أغنية حماسية وإيقاعية مميزة ألهبت حماس الجماهير وأصبحت أنشودة المهرجانات والحفلات والترند الأول في تيك توك وإنستغرام.",
            lyrics = """انت تدري شكد احبك
لو تحسب نجوم السما
ما يوصل لحبي ابد
يا نبض روحي والدمى

اصعد للكمر وافرشلك نجوم
واكلك يا حبيبي حبنا يدوم
انت النفس اللي بيه اعيش
بدونك اني ما ادري ليش

حبي الك ما ينتهي
ولا يقل بناره ينكوي
انت حبيبي للابد
ماكو مثلك اي احد""",
            composer = "علي صابر",
            lyricist = "حيدر الأسير",
            isNewRelease = true,
            gradientStartHex = 0xFFB71C1C,
            gradientEndHex = 0xFF210505,
            youtubeVideoId = "O9jF4w1_kEU",
            likesCount = "990K إعجاب",
            commentsCount = "46.1K تعليق",
            channelSubscribers = "5.8M مشترك",
            publishDate = "15 يوليو 2022",
            trendingRank = "#2 تريند الموسيقى والمقاطع القصيرة"
        ),
        Song(
            id = "mako_menni",
            title = "Mako Menni",
            arabicTitle = "ماكو مني",
            category = SongCategory.CLIPS,
            durationSeconds = 230,
            durationFormatted = "3:50",
            viewsCount = "92,300,000 مشاهدة",
            releaseYear = "2020",
            description = "أغنية تعبر عن عزة النفس وقوة الشخصية، نالت إعجاب الملايين وتجاوزت حدود الوطن العربي.",
            lyrics = """ماكو مني بهاي الدنيا
حبني واعرف قيمتي
اني اللي ضحيت بروحي
وانت ضيعت فرصتي

روح دور تلكى مثلي؟
والله حيرة لو لكيت
اني شمعة بنص ظلامك
بس انت اللي طفيت

راح تعرف شكد خسرت
من يفوت الفوت وتندم
اني تاج على الراس
وانت بغيابي تنهدم""",
            composer = "علي صابر",
            lyricist = "رامي العبودي",
            isNewRelease = false,
            gradientStartHex = 0xFF6A1B9A,
            gradientEndHex = 0xFF12001F,
            youtubeVideoId = "B9v8CvhE2qQ",
            likesCount = "780K إعجاب",
            commentsCount = "39.5K تعليق",
            channelSubscribers = "5.8M مشترك",
            publishDate = "1 يناير 2020",
            trendingRank = "#4 في الكليبات العربية الأكثر مشاهدة"
        ),
        Song(
            id = "hayra",
            title = "Hayra",
            arabicTitle = "حيرة (مسلسل حيرة)",
            category = SongCategory.CLIPS,
            durationSeconds = 275,
            durationFormatted = "4:35",
            viewsCount = "66,900,000 مشاهدة",
            releaseYear = "2022",
            description = "شارة المسلسل العراقي الشهير 'حيرة'، من أروع الأغاني الدرامية المؤثرة بصوت رحمة رياض.",
            lyrics = """حيرة وظلمة هالدنيا
وشوك يعذب الروح
وين اروح واني ادور
بلسم يداوي الجروح

كل خطوة بألف عبرة
والزمن ما يرحم احد
الي ضحك بالبداية
ترا يبجي بالاخير اشد

يا حيرة الماضي التعبني
والحاضر المليان اسرار
اني شسويت بحياتي
حتى اعيش بنار بنار""",
            composer = "علي صابر",
            lyricist = "رامي العبودي",
            isNewRelease = false,
            gradientStartHex = 0xFF0D47A1,
            gradientEndHex = 0xFF001026,
            youtubeVideoId = "K0b7oXU01bE",
            likesCount = "590K إعجاب",
            commentsCount = "31.4K تعليق",
            channelSubscribers = "5.8M مشترك",
            publishDate = "18 سبتمبر 2022",
            trendingRank = "أفضل تتر وموسيقى تصويرية لمسلسل درامي"
        ),
        Song(
            id = "sabaa_alsaada",
            title = "Helw Hal Sheour",
            arabicTitle = "حلو هالشعور",
            category = SongCategory.LATEST,
            durationSeconds = 198,
            durationFormatted = "3:18",
            viewsCount = "55,200,000 مشاهدة",
            releaseYear = "2023",
            description = "أحدث إبداعات رحمة رياض بأسلوب عصري راقص ومبهج ينبض بالفرح والرومانسية الدافئة.",
            lyrics = """حلو هالشعور البيك احسه
قلبي غرق وما لقى مرسى
غير بعيونك وحضنك دفا
يا تاج راسي واصل الوفا

سبع السعادة وياك انفتحن بيبانه
روحي بحبك طارت فرحانة
يل وجهك يجيب الخير والراحة
وياك الدنيا جنة وسياحة""",
            composer = "محب الراوي",
            lyricist = "قصي عيسى",
            isNewRelease = true,
            gradientStartHex = 0xFFE65100,
            gradientEndHex = 0xFF240800,
            youtubeVideoId = "3A0C8-dI1k8",
            likesCount = "460K إعجاب",
            commentsCount = "25.0K تعليق",
            channelSubscribers = "5.8M مشترك",
            publishDate = "14 فبراير 2023",
            trendingRank = "#3 تريند الأغاني الرومانسية"
        ),
        Song(
            id = "kalam_alhub",
            title = "Kalam Al Hub",
            arabicTitle = "كلام الحب (جلسة)",
            category = SongCategory.SESSIONS,
            durationSeconds = 310,
            durationFormatted = "5:10",
            viewsCount = "43,100,000 مشاهدة",
            releaseYear = "2021",
            description = "جلسة طربية أصيلة استعرضت فيها رحمة رياض مساحات صوتها الدافئ والشجن العراقي التراثي الأصيل.",
            lyrics = """كلام الحب كلو لعيونك
روحي فداك وما اعيش بدونك
يل طيفك ساكن خيالي
وانت العمر ورسم الليالي

يا عازف ع اوتار قلبي
رفقا بحالي وبالعشك مالتي
خلي ايدك بايدي ونمشي
انت حياتي ودنيتي وكلشي""",
            composer = "فلكلور عراقي مطور",
            lyricist = "تراث بغدادي",
            isNewRelease = false,
            gradientStartHex = 0xFF3E2723,
            gradientEndHex = 0xFF140B09,
            youtubeVideoId = "v9GZ6B_w0QY",
            likesCount = "395K إعجاب",
            commentsCount = "22.1K تعليق",
            channelSubscribers = "5.8M مشترك",
            publishDate = "2021",
            trendingRank = "روائع الجلسات الطربية العراقية"
        ),
        Song(
            id = "ani_alaqwa",
            title = "Ani El Aqwa",
            arabicTitle = "اني الأقوى",
            category = SongCategory.CLIPS,
            durationSeconds = 240,
            durationFormatted = "4:00",
            viewsCount = "41,700,000 مشاهدة",
            releaseYear = "2019",
            description = "أغنية حماسية تمثل صمود المرأة وقوتها في وجه التحديات والظروف الصعبة.",
            lyrics = """ما انكسر لو مهما صار
اني الجبل واني القرار
لو طاحت نجوم السما
يبقى اسمي عالي ومنار

اني الاقوى بهاي المعركة
ودمعي ابد ما اذرقه
خليهم يحجون شعلية
اني الشمس وهي الضوية""",
            composer = "علي صابر",
            lyricist = "حيدر الأسير",
            isNewRelease = false,
            gradientStartHex = 0xFF880E4F,
            gradientEndHex = 0xFF1A000E,
            youtubeVideoId = "W7H-B4sT2sE",
            likesCount = "325K إعجاب",
            commentsCount = "19.3K تعليق",
            channelSubscribers = "5.8M مشترك",
            publishDate = "2019",
            trendingRank = "رمز التحدي والقوة النسائية"
        ),
        Song(
            id = "khoya",
            title = "Light The Sky",
            arabicTitle = "Light The Sky (كأس العالم)",
            category = SongCategory.DUETS,
            durationSeconds = 250,
            durationFormatted = "4:10",
            viewsCount = "86,500,000 مشاهدة",
            releaseYear = "2022",
            description = "الأغنية الرسمية لكأس العالم FIFA قطر 2022 بمشاركة رحمة رياض مع نخبة من نجمات الفن العالمي.",
            lyrics = """Light the sky, shout it loud
Together we make our nation proud
من العراق لكل العالم
صوت الفرح عالي وغالي""",
            composer = "RedOne",
            lyricist = "RedOne & International Writers",
            isNewRelease = false,
            gradientStartHex = 0xFF2E7D32,
            gradientEndHex = 0xFF081C09,
            youtubeVideoId = "x6w-o0tK8a4",
            likesCount = "950K إعجاب",
            commentsCount = "43.2K تعليق",
            channelSubscribers = "FIFA & Rahma Riad",
            publishDate = "7 أكتوبر 2022",
            trendingRank = "#1 في افتتاح كأس العالم 2022"
        ),
        Song(
            id = "duet_babylon",
            title = "Babylon Festival Live",
            arabicTitle = "ميدلي بابل الدولي (لايف)",
            category = SongCategory.CONCERTS,
            durationSeconds = 480,
            durationFormatted = "8:00",
            viewsCount = "76,100,000 مشاهدة",
            releaseYear = "2023",
            description = "حفل مباشر أسطوري وسط حضور جماهيري تجاوز 20 ألف متفرج في مسرح بابل الأثري العريق.",
            lyrics = """(ميدلي غنائي يضم باقة من أروع أغاني رحمة رياض ووالدها الراحل رياض أحمد وسط تفاعل جماهيري تاريخي)
- واجب بالروح اداريك
- مجرد كلام
- والكوكب و وعد مني""",
            composer = "توزيع فرقة بابل الموسيقية",
            lyricist = "نخبة من الشعراء العراقيين",
            isNewRelease = false,
            gradientStartHex = 0xFFD84315,
            gradientEndHex = 0xFF240A00,
            youtubeVideoId = "R2i-9qN7XQk",
            likesCount = "680K إعجاب",
            commentsCount = "34.8K تعليق",
            channelSubscribers = "5.8M مشترك",
            publishDate = "2023",
            trendingRank = "أكبر حفل جماهيري في العراق"
        )
    )

    // Rahma Riad Shorts & Instagram Reels
    val reels: List<RahmaReel> = listOf(
        RahmaReel(
            id = "reel_1",
            title = "كون نصير اني وياك نجمة بالسما ✨",
            caption = "أجمل لحظات غناء الكوكب مع الجمهور العراقي والعربي! #رحمة_رياض #الكوكب #عراق #Shorts #Reels",
            youtubeVideoId = "VlhKzTzVfqg",
            viewsCount = "5.2M",
            likesCount = "482K",
            initialLikes = 482000,
            commentsCount = "12.8K",
            sharesCount = "48.1K",
            songTitle = "رحمة رياض - الكوكب",
            dateText = "تريند #1",
            tags = listOf("الكوكب", "رحمة_رياض", "تريند_تيكتوك", "ريلز")
        ),
        RahmaReel(
            id = "reel_2",
            title = "أصعد للكمر وافرشلك نجوم 🌙",
            caption = "تريند أصعد للكمر يكتسح منصات التواصل! هل حفظتم حركات الرقصة؟ #أصعد_للكمر #رحمة_رياض #تريند",
            youtubeVideoId = "O9jF4w1_kEU",
            viewsCount = "7.8M",
            likesCount = "624K",
            initialLikes = 624000,
            commentsCount = "18.4K",
            sharesCount = "79.2K",
            songTitle = "رحمة رياض - أصعد للكمر",
            dateText = "شائع جداً",
            tags = listOf("أصعد_للكمر", "شورتس", "ميوزك", "إنستغرام")
        ),
        RahmaReel(
            id = "reel_3",
            title = "وعد مني بعد ما انطي قلبي 💔",
            caption = "المقطع الأكثر تأثيراً وطلباً في كل الحفلات.. إحساس وصوت لا يتكرر #وعد_مني #رحمة_رياض",
            youtubeVideoId = "jD1JqL0K2tM",
            viewsCount = "4.3M",
            likesCount = "390K",
            initialLikes = 390000,
            commentsCount = "9.5K",
            sharesCount = "31.7K",
            songTitle = "رحمة رياض - وعد مني",
            dateText = "ذكريات ذهبية",
            tags = listOf("وعد_مني", "شجن_عراقي", "ريلز")
        ),
        RahmaReel(
            id = "reel_4",
            title = "ماكو مني بهاي الدنيا.. عزة نفس وفخر 👑",
            caption = "لكل شخص يعرف قيمته وما يرضى بالقليل! أنتم التاج على الراس #ماكو_مني #رحمة_رياض #قوة",
            youtubeVideoId = "B9v8CvhE2qQ",
            viewsCount = "3.9M",
            likesCount = "315K",
            initialLikes = 315000,
            commentsCount = "7.8K",
            sharesCount = "24.5K",
            songTitle = "رحمة رياض - ماكو مني",
            dateText = "الأكثر مشاركة",
            tags = listOf("ماكو_مني", "عزة_نفس", "شورتس")
        ),
        RahmaReel(
            id = "reel_5",
            title = "إحساس مسلسل حيرة يبكي الملايين 🥺",
            caption = "شارة مسلسل حيرة بلمسة رحمة رياض التي تلامس القلوب وتوجع الروح #حيرة #دراما #رحمة_رياض",
            youtubeVideoId = "K0b7oXU01bE",
            viewsCount = "3.1M",
            likesCount = "278K",
            initialLikes = 278000,
            commentsCount = "6.2K",
            sharesCount = "19.8K",
            songTitle = "رحمة رياض - حيرة",
            dateText = "دراما",
            tags = listOf("حيرة", "مسلسلات_عراقية", "ريلز")
        ),
        RahmaReel(
            id = "reel_6",
            title = "كواليس مشاركتي في كأس العالم Light The Sky ⚽",
            caption = "فخر العراق في أكبر محفل عالمي.. لحظات لا تُنسى من الدوحة! #كأس_العالم #رحمة_رياض #FIFA",
            youtubeVideoId = "x6w-o0tK8a4",
            viewsCount = "8.9M",
            likesCount = "810K",
            initialLikes = 810000,
            commentsCount = "26.3K",
            sharesCount = "112K",
            songTitle = "Rahma Riad - Light The Sky (FIFA)",
            dateText = "عالمي",
            tags = listOf("كأس_العالم", "FIFA", "فخر_العراق", "Shorts")
        )
    )

    val sampleComments: List<ReelComment> = listOf(
        ReelComment("c1", "علي البصراوي ✪", "صوت وإحساس لا يوصف، فخر العراق يا رحمة! ❤️🇮🇶", "منذ ساعة", "1.4K"),
        ReelComment("c2", "مريم الكردي", "أحلى كليب وأحلى إطلالة.. الكوكب أغنية القرن 🌟", "منذ 3 ساعات", "940"),
        ReelComment("c3", "أحمد السامرائي", "تطبيق RahmaRiad من تصميم ✪Mister Ali ممتاز جداً وخاصية الشورتس عالمية!", "منذ 5 ساعات", "2.1K"),
        ReelComment("c4", "نور الهدى", "أصعد للكمر وافرشلك نجوم.. الكلمات تدخل القلب مباشرة 😍", "منذ يوم", "620"),
        ReelComment("c5", "حيدر البغدادي", "الله يرحم والدك رياض أحمد ويرزقك التوفيق يا بنت الأصايل 👑", "منذ يومين", "1.8K")
    )

    fun getSongById(id: String): Song? = songs.find { it.id == id }

    fun filterByCategory(category: SongCategory): List<Song> {
        return if (category == SongCategory.ALL) {
            songs
        } else {
            songs.filter { it.category == category }
        }
    }

    fun searchSongs(query: String): List<Song> {
        if (query.isBlank()) return songs
        val lower = query.trim().lowercase()
        return songs.filter {
            it.title.lowercase().contains(lower) ||
            it.arabicTitle.contains(lower) ||
            it.lyrics.contains(lower) ||
            it.composer.contains(lower) ||
            it.description.contains(lower)
        }
    }
}
