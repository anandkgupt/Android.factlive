package com.example.data.repository

import com.example.data.api.GeminiApiClient
import com.example.data.api.FactCheckResult
import com.example.data.db.FactCheckDao
import com.example.data.db.SavedFactCheck
import kotlinx.coroutines.flow.Flow

// Static structure representing standard feeds from Fact Live
data class LiveFactFeedItem(
    val id: String,
    val claim: String,
    val verdict: String,         // True, False, Misleading, Partially True
    val summary: String,
    val date: String,
    val isRegional: Boolean,     // True if regional / language Malayalam, False if English
    val bannerUrl: String? = null,
    val category: String,        // Social Media, Politics, Health, Science
    val details: String,
    val context: String,
    val references: String
)

class FactCheckRepository(private val factCheckDao: FactCheckDao) {

    // Access local Room bookmarks reactively
    val allSavedChecks: Flow<List<SavedFactCheck>> = factCheckDao.getAllSavedChecks()

    suspend fun saveCheck(check: SavedFactCheck) {
        factCheckDao.insertCheck(check)
    }

    suspend fun removeCheckById(id: Int) {
        factCheckDao.deleteCheckById(id)
    }

    suspend fun removeCheckByClaim(claim: String) {
        factCheckDao.deleteCheckByClaim(claim)
    }

    suspend fun isClaimSaved(claim: String): Boolean {
        return factCheckDao.isClaimSaved(claim)
    }

    /**
     * Conducts a real-time fact check using the Gemini API client.
     */
    suspend fun checkClaimLive(claimText: String): FactCheckResult {
        return GeminiApiClient.factCheckClaim(claimText)
    }

    /**
     * Preloaded fact-checking items representing what you'd find on www.factlive.in
     */
    fun getPresetDebunks(): List<LiveFactFeedItem> {
        return listOf(
            LiveFactFeedItem(
                id = "feed_1",
                claim = "നാലുവരിപ്പാതയിൽ വെള്ളക്കെട്ട് ഒഴിവാക്കാൻ പൈപ്പുകൾ വഴി വെള്ളം ഒഴുക്കി വിടുന്നു.",
                verdict = "False",
                summary = "മഴക്കാലത്ത് ദേശീയപാതയിലുണ്ടായ വെള്ളക്കെട്ട് പൈപ്പ് കൊണ്ട് ഒഴുക്കി പരസ്യമാക്കിയെന്ന വാദം വ്യാജമാണ്. ഇത് ജല അതോറിറ്റിയുടെ പ്രധാന ലൈൻ അറ്റകുറ്റപ്പണിയുടെ അരികിൽ പരീക്ഷണാടിസ്ഥാനത്തിൽ മാറ്റിയതാണ്.",
                date = "ഇന്ന് (Today)",
                isRegional = true,
                category = "സോഷ്യൽ മീഡിയ (Social Media)",
                details = "കേരളത്തിലെ പ്രശസ്തമായ ദേശീയപാത 66 ലാണ് ഈ സംഭവം എന്ന് കാണിച്ചാണ് വീഡിയോ വ്യാപകമായി പ്രചരിച്ചത്. പുതിയ ഹൈവേ നിർമ്മാണത്തിലെ അശാസ്ത്രീയത മൂലമാണ് പൈപ്പ് വഴി റോഡിൽ നിന്നും വെള്ളം കളയുന്നത് എന്നായിരുന്നു വാദം. എന്നാൽ വസ്തുത പരിശോധനയിൽ ഇത് അറ്റകുറ്റപ്പണികളിൽ ഉണ്ടായ സ്വാഭാവിക പൈപ്പ് മാറ്റം മാത്രമാണെന്ന് കണ്ടെത്തി.",
                context = "ലാൻഡ്സ്കേപ്പിംഗ് ഭംഗിയില്ലാത്ത നിർമ്മാണം എന്ന ക്യാപ്ഷനോടെ വാട്ട്സ്ആപ്പ്, ഫെയ്സ്ബുക്ക് ഗ്രൂപ്പുകളിൽ വീഡിയോ പ്രചരിച്ചു.",
                references = "1. National Highway Authority of India (NHAI) Regional Directorate\n2. Kerala Water Authority (KWA) Local Assistant Engineer Statement\n3. Fact Live Ground Verification Team report"
            ),
            LiveFactFeedItem(
                id = "feed_2",
                claim = "Viral video shows a double sun anomaly in the sky in northern Canada.",
                verdict = "Misleading",
                summary = "The video claiming to depict a 'double sun' phenomenon is not fake, but it is an optical illusion called 'Sun Dog' or parhelion, caused by ice crystals refracting light.",
                date = "Yesterday",
                isRegional = false,
                category = "Science",
                details = "This spectacular visual phenomenon was captured on video and quickly labeled by conspiracy blogs as evidence of Nibiru or a second sun. Research into meteorological recordings confirms it was a typical ice parhelion, which naturally occurs in ultra-cold high-latitude regions when light interacts with plate-shaped hexagonal ice crystals.",
                context = "Shared over 1.2 million times across Facebook, TikTok, and X (formerly Twitter).",
                references = "1. World Meteorological Organization (WMO) - Ice Refraction Guides\n2. Atmospheric Optics Database - Canada Observations, June 2026\n3. NASA Scientific Visualization Studio sun refraction guide"
            ),
            LiveFactFeedItem(
                id = "feed_3",
                claim = "WHO പ്രഖ്യാപിച്ച പുതിയ കോവിഡ് അടിയന്തരാവസ്ഥ കാരണം ലോക്ക്ഡൗൺ അടിച്ചേൽപ്പിക്കുന്നു.",
                verdict = "False",
                summary = "WHO കോവിഡിന്റെ പുതിയ വേരിയന്റുകളെ നിരീക്ഷിക്കുന്നുണ്ടെങ്കിലും ആഗോള ലോക്ക്ഡൗണോ അടിയന്തരാവസ്ഥയോ പ്രഖ്യാപിച്ചിട്ടില്ല. പ്രചരിക്കുന്നത് വ്യാജ വാർത്തയാണ്.",
                date = "2 days ago",
                isRegional = true,
                category = "മരുന്ന് & ആരോഗ്യം (Health)",
                details = "പഴയ 2020-ലെ ഓഡിയോ സന്ദേശം എഡിറ്റ് ചെയ്ത് പുതിയ തീയതികളോടെ ചിലർ സോഷ്യൽ മീഡിയയിൽ പ്രചരിപ്പിക്കുകയായിരുന്നു. ആളുകളിൽ ഭീതി വളർത്തുന്നതിന് വേണ്ടി ദുരുദ്ദേശത്തോടെ മെസ്സേജ് ഫോർവേഡ് ചെയ്യാൻ പ്രേരിപ്പിക്കുന്ന രീതിയിലാണ് ഇത് രൂപകൽപ്പന ചെയ്തത്.",
                context = "വാട്ട്സ്ആപ്പിൽ ഒരു പുരുഷന്റെ ശബ്ദ സന്ദേശ രൂപത്തിലാണ് ഇത് ഏറെ പ്രചരിച്ചത്.",
                references = "1. World Health Organization (WHO) Official Media Communique\n2. Min. of Health and Family Welfare, Govt of India Advisories\n3. Press Information Bureau (PIB) Fact Check Center"
            ),
            LiveFactFeedItem(
                id = "feed_4",
                claim = "The Eiffel Tower was seen engulfed in massive flames and smoke in Paris.",
                verdict = "False",
                summary = "Photos showing the Eiffel Tower on fire are AI-generated images that initially circulated on TikTok and spread across other platforms.",
                date = "3 days ago",
                isRegional = false,
                category = "World Hoaxes",
                details = "Generative AI tools were used to create high-concept images of smoke plumes and yellow/red flames engulfing the structure. Official Paris authorities, local law enforcement, and real-time live-streams of the tower confirmed no fire or incidents occurred.",
                context = "Originally appeared in a TikTok slideshow captioned 'Paris is burning' before being picked up by massive international fanbases.",
                references = "1. Paris Police Prefecture Official Twitter Handler (@prefpolice)\n2. Live Webcams of the Champ de Mars, Paris\n3. AI detection metadata parsing via Hugging Face tools"
            ),
            LiveFactFeedItem(
                id = "feed_5",
                claim = "ഇരട്ടക്കുട്ടികൾക്ക് ജന്മം നൽകിയാൽ കേരള സർക്കാർ ഇരുപത്തിയയ്യായിരം രൂപ ധനസഹായം നൽകുന്നു.",
                verdict = "Partially True",
                summary = "പ്രസവ ധനസഹായ പദ്ധതികൾ ഉണ്ടെങ്കിലും സാധാരണ നിലയിൽ ഇരട്ടക്കുട്ടികൾക്ക് മാത്രമായി ഇത്രയും തുക പ്രത്യേകമായി ഒറ്റത്തവണ നൽകുന്ന ഒരു പുതിയ സ്കീമും സർക്കാർ പ്രഖ്യാപിച്ചിട്ടില്ല.",
                date = "4 days ago",
                isRegional = true,
                category = "സർക്കാർ പദ്ധതികൾ (Politics)",
                details = "സാമൂഹ്യനീതി വകുപ്പിന്റെയും വനിതാ ശിശുവികസന വകുപ്പിന്റെയും കീഴിൽ നിരവധി സാമ്പത്തിക ആനുകൂല്യങ്ങളുണ്ട് (മാതൃവന്ദന തുടങ്ങിയവ), പക്ഷെ അവ മുൻഗണനാവിഭാഗങ്ങൾക്കും പ്രസവത്തിന്റെ പൊതുവായ സുരക്ഷയ്ക്കുമാണ്. ഇരട്ടക്കുട്ടികൾക്ക് ലംപ്-സം 25,000 രൂപ എന്ന രീതിയിൽ വരുന്നത് തെറ്റായ പ്രചാരണമാണ്.",
                context = "സോഷ്യൽ വെൽഫെയർ ആവിഷ്കാര അപേക്ഷകൾ എന്ന വ്യാജ വെബ്സൈറ്റ് ലിങ്കുകൾക്കൊപ്പം ലിങ്ക് ക്ലിക്കുകൾ വർദ്ധിപ്പിക്കാനാണ് ഇത് പ്രചരിപ്പിച്ചത്.",
                references = "1. Directorate of Social Justice, Govt of Kerala Schemelists\n2. Public Relations Department (PRD) Kerala Fact Check Portal\n3. Ministry of Women and Child Development Guidelines"
            )
        )
    }
}
