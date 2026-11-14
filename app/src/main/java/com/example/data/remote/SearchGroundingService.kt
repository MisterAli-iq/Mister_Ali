package com.example.data.remote

import android.util.Log
import com.example.model.LinkSourceType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.GroundingMetadata
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "SearchGroundingService"
private const val GROUNDED_MODEL_NAME = "gemini-3.8-flash"
private const val INFERENCE_TIMEOUT_MS = 60_000L

data class GroundedWebSource(
    val title: String,
    val uri: String,
    val domain: String
)

sealed interface GroundedSearchUiState {
    data object Idle : GroundedSearchUiState
    data class Loading(val query: String) : GroundedSearchUiState
    data class Streaming(
        val query: String,
        val text: String,
        val webQueries: List<String> = emptyList(),
        val sources: List<GroundedWebSource> = emptyList()
    ) : GroundedSearchUiState

    data class Success(
        val query: String,
        val text: String,
        val webQueries: List<String> = emptyList(),
        val sources: List<GroundedWebSource> = emptyList()
    ) : GroundedSearchUiState

    data class Error(
        val query: String,
        val message: String
    ) : GroundedSearchUiState
}

object SearchGroundingService {

    private val groundedModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = GROUNDED_MODEL_NAME,
            tools = listOf(Tool.googleSearch()),
            systemInstruction = content {
                text(
                    "أنت باحث ذكي متخصص في الفنانة العراقية رحمة رياض (Rahma Riad) وقناتها الرسمية @RahmaRiad وحسابها الرسمي على إنستغرام instagram.com/rahmariadh (@rahmariadh)، " +
                        "وأغانيها وألبوماتها وحفلاتها ومهرجاناتها وبرامجها التلفزيونية وآخر أخبارها الفنية. " +
                        "استخدم أداة البحث في Google دائماً للحصول على أحدث المعلومات الموثوقة والتواريخ الحقيقية والروابط المباشرة. " +
                        "أجب باللغة العربية بشكل مرتب وموجز ومباشر مع ذكر التفاصيل الحديثة والروابط أو الجهات الرسمية عند توفرها."
                )
            }
        )
    }

    fun buildSearchPrompt(
        rawQuery: String,
        enabledSources: List<LinkSourceType> = LinkSourceType.twelvePlatforms
    ): String {
        val cleaned = rawQuery.trim()
        val scopedPlatforms = enabledSources
            .filter { it != LinkSourceType.OTHER }
        val scopeHint = when {
            scopedPlatforms.isEmpty() || scopedPlatforms.size >= LinkSourceType.twelvePlatforms.size ->
                " (يشمل يوتيوب @RahmaRiad وإنستغرام instagram.com/rahmariadh والمصادر الرسمية)."
            scopedPlatforms.size == 1 -> {
                val single = scopedPlatforms.first()
                val extra = if (single == LinkSourceType.INSTAGRAM) " instagram.com/rahmariadh" else ""
                " ركّز البحث حصرياً على مصدر: ${single.titleArabic} (${single.hostsSummary}$extra)."
            }
            else ->
                " ركّز البحث على المصادر المحددة فقط: ${scopedPlatforms.joinToString("، ") { "${it.titleArabic} (${it.hostsSummary})" }}."
        }

        if (cleaned.isBlank()) {
            return "ما هي أحدث أغاني وحفلات ومنشورات وأخبار الفنانة رحمة رياض (@RahmaRiad و instagram.com/rahmariadh) مؤخراً؟ استخدم بحث Google لعرض أحدث النتائج الموثوقة.$scopeHint"
        }
        val mentionsArtist = cleaned.contains("رحمة") ||
            cleaned.contains("rahma", ignoreCase = true) ||
            cleaned.contains("@rahmariad", ignoreCase = true)
        return if (mentionsArtist) {
            "ابحث في Google وقدّم معلومات محدثة ودقيقة ونتائج قريبة وذات صلة حول: \"$cleaned\" مع إبراز التواريخ والأعمال والروابط المباشرة.$scopeHint"
        } else {
            "ابحث في Google وقدّم معلومات محدثة ودقيقة ونتائج قريبة وذات صلة عن الفنانة رحمة رياض (@RahmaRiad) فيما يخص: \"$cleaned\" (مثل الأغنية، الحفل، الملحن/الشاعر، تاريخ الإصدار، أو المقابلات الرسمية).$scopeHint"
        }
    }

    fun extractGroundingData(metadata: GroundingMetadata?): Pair<List<String>, List<GroundedWebSource>> {
        if (metadata == null) return emptyList<String>() to emptyList()
        val queries = metadata.webSearchQueries
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val sources = metadata.groundingChunks
            .mapNotNull { chunk -> chunk.web }
            .mapNotNull { web ->
                val uri = web.uri?.trim().orEmpty()
                if (uri.isBlank()) return@mapNotNull null
                val domain = web.domain?.trim().orEmpty()
                val title = web.title?.trim()?.takeIf { it.isNotBlank() }
                    ?: domain.takeIf { it.isNotBlank() }
                    ?: uri
                GroundedWebSource(title = title, uri = uri, domain = domain)
            }
            .distinctBy { it.uri }
        return queries to sources
    }

    fun streamGroundedSearch(
        rawQuery: String,
        enabledSources: List<LinkSourceType> = LinkSourceType.twelvePlatforms
    ): Flow<GroundedSearchUiState> = flow {
        val displayQuery = rawQuery.trim().ifBlank { "أحدث أخبار وأعمال رحمة رياض @RahmaRiad" }
        emit(GroundedSearchUiState.Loading(displayQuery))

        val prompt = buildSearchPrompt(displayQuery, enabledSources)
        val accumulatedText = StringBuilder()
        var capturedQueries: List<String> = emptyList()
        var capturedSources: List<GroundedWebSource> = emptyList()

        try {
            withTimeout(INFERENCE_TIMEOUT_MS) {
                groundedModel.generateContentStream(prompt).collect { chunk ->
                    val delta = chunk.text.orEmpty()
                    if (delta.isNotEmpty()) {
                        accumulatedText.append(delta)
                    }
                    val metadata = chunk.candidates.firstOrNull()?.groundingMetadata
                    if (metadata != null) {
                        val (queries, sources) = extractGroundingData(metadata)
                        if (queries.isNotEmpty()) capturedQueries = queries
                        if (sources.isNotEmpty()) capturedSources = sources
                    }
                    if (accumulatedText.isNotBlank() || capturedSources.isNotEmpty()) {
                        emit(
                            GroundedSearchUiState.Streaming(
                                query = displayQuery,
                                text = accumulatedText.toString(),
                                webQueries = capturedQueries,
                                sources = capturedSources
                            )
                        )
                    }
                }
            }

            val finalAnswer = accumulatedText.toString().trim()
            if (finalAnswer.isNotBlank() || capturedSources.isNotEmpty()) {
                emit(
                    GroundedSearchUiState.Success(
                        query = displayQuery,
                        text = finalAnswer.ifBlank { "تم العثور على مصادر مرتبطة في بحث Google أدناه." },
                        webQueries = capturedQueries,
                        sources = capturedSources
                    )
                )
            } else {
                emit(
                    GroundedSearchUiState.Error(
                        query = displayQuery,
                        message = "لم يُرجع البحث المباشر نتائج نصية لهذا الاستعلام؛ جرب صياغة أخرى."
                    )
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Grounded search timed out after 60s for query=$displayQuery", e)
            emit(
                GroundedSearchUiState.Error(
                    query = displayQuery,
                    message = "انتهت مهلة البحث المباشر عبر Google (60 ثانية). تحقق من الاتصال وأعد المحاولة."
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Grounded search failed for query=$displayQuery", e)
            emit(
                GroundedSearchUiState.Error(
                    query = displayQuery,
                    message = e.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "تعذر إتمام البحث المباشر عبر Google حالياً."
                )
            )
        }
    }
}
