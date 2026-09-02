# RTL AI Tutor — Complete Project Roadmap v5
**IIIT Hyderabad · 4-Member Team · 8-Week Sprint starting Sep 3, 2026**
*Deadline: Nov 10, 2026 · 4 Hard Deliverables, one every 2 weeks*

---

# ═══════════════════════════════════════════
# TODAY'S TASKS (Sep 2, 2026 — Before Timeline Starts)
# ═══════════════════════════════════════════

Today has three jobs. Do them in this order. The 8-week timeline starts tomorrow.

---

## TODAY TASK 1 — Load the LLM onto the QIDK HDK8650 MP

### What the HDK8650 MP Is
The Qualcomm HDK8650 MP is the development board for the **Snapdragon 8 Gen 3** chip. It runs Android. The 8 Gen 3 has a Hexagon NPU (HTP), but as of 2026, **Qualcomm AI Hub no longer offers pre-compiled LLM packages for the 8 Gen 3 chipset** — all LLMs on AI Hub (Llama 3.2, Qwen3, Phi-4, etc.) now require **Snapdragon 8 Elite or newer**.

> **Confirmed from aihub.qualcomm.com:** Llama v3.2 3B, Phi-4-Mini, Qwen3, Llama v3.1 8B — none list Snapdragon 8 Gen 3 in their supported chipsets. The minimum is now Snapdragon 8 Elite (sm8750).

**The solution: use `llama.cpp` with a GGUF-quantized model**, which runs on the 8 Gen 3's **GPU (Adreno 750)** and **CPU (Cortex-X4)** with excellent performance. This is a well-supported, production-ready path. Expected inference: ~15–25 tokens/s on Adreno 750 GPU with a 3B model — comfortably under 3s for typical RTL tutor responses.

**Chosen model: Llama-3.2-3B-Instruct (Q4_K_M GGUF)** — same model family as originally planned, same capability, different deployment format.

---

### Approach A — llama.cpp on Android (GPU/CPU, Recommended)

This runs the model directly on the device using llama.cpp compiled for Android.

#### Prerequisites (macOS or Linux host)
```bash
# Install Android NDK (needed to cross-compile llama.cpp for Android)
# macOS:
brew install android-platform-tools
# Download Android NDK r26 or later from:
# https://developer.android.com/ndk/downloads
# Extract to ~/android-ndk-r26d (or wherever you like)

# Linux:
sudo apt install -y android-tools-adb android-tools-fastboot wget unzip
wget https://dl.google.com/android/repository/android-ndk-r26d-linux.zip
unzip android-ndk-r26d-linux.zip

# Set NDK path
export NDK=~/android-ndk-r26d   # adjust to your path
```

#### Step 1 — Build llama.cpp for Android ARM64
```bash
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp

# Build with Vulkan backend (targets Adreno 750 GPU — best performance on 8 Gen 3)
cmake -B build-android \
  -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-28 \
  -DGGML_VULKAN=ON \
  -DCMAKE_BUILD_TYPE=Release

cmake --build build-android --config Release -j$(nproc)
# Output binary: build-android/bin/llama-cli
```

#### Step 2 — Download the GGUF Model
```bash
# Download Llama-3.2-3B-Instruct Q4_K_M (approx 2.0 GB) — best quality/speed tradeoff
# On macOS:
brew install huggingface-cli
# On Linux:
pip install huggingface_hub

# Download (free, no account needed for this model):
huggingface-cli download \
  bartowski/Llama-3.2-3B-Instruct-GGUF \
  Llama-3.2-3B-Instruct-Q4_K_M.gguf \
  --local-dir ~/llama_model/

# File will be at: ~/llama_model/Llama-3.2-3B-Instruct-Q4_K_M.gguf (~2.0 GB)
```

#### Step 3 — Connect HDK8650 and Push Files
```bash
# Connect the HDK8650 via USB-C cable
# On device: Settings → About Phone → tap Build Number 7x → Developer Options → Enable USB Debugging

adb devices
# Should show: 1234567890ABCDEF    device
# If "unauthorized": tap "Allow" on the device screen

# Check storage first (need ~2.5 GB free)
adb shell df -h /data

# Push the compiled llama-cli binary
adb push build-android/bin/llama-cli /data/local/tmp/
adb shell chmod +x /data/local/tmp/llama-cli

# Push the model
adb push ~/llama_model/Llama-3.2-3B-Instruct-Q4_K_M.gguf /data/local/tmp/

# Verify
adb shell ls -lh /data/local/tmp/
```

#### Step 4 — Test Inference on Device
```bash
adb shell

# Run a quick test (GPU via Vulkan — uses Adreno 750)
/data/local/tmp/llama-cli \
  -m /data/local/tmp/Llama-3.2-3B-Instruct-Q4_K_M.gguf \
  -ngl 99 \
  -n 100 \
  --prompt "You are an RTL circuit tutor. Explain what a flip-flop is using a guiding question."

# -ngl 99 = offload all 99 layers to GPU (Adreno 750 via Vulkan)
# Expected output: ~15–25 tokens/sec — first response in ~2–3s
# If Vulkan is unsupported, remove -ngl 99 (falls back to CPU, ~5 tok/s)

exit
```

---

### Approach B — llama.cpp via MLC-LLM Android App (Easier, Less Code)

If cross-compiling feels risky on Day 0, MLC-LLM provides a pre-built Android APK that wraps llama.cpp with OpenCL/Vulkan GPU support and a local HTTP server your app can hit.

```bash
# 1. Download MLC-LLM APK from:
#    https://github.com/mlc-ai/mlc-llm/releases
#    (look for mlc-llm-android-*.apk)

# 2. Install on device:
adb install mlc-llm-android.apk

# 3. Inside MLC-LLM app → Add Model → paste Hugging Face URL:
#    mlc-ai/Llama-3.2-3B-Instruct-q4f16_1-MLC
#    (app downloads and quantizes automatically)

# 4. Start local server inside MLC-LLM app (Settings → Enable REST API → port 8080)

# 5. Your Android app can now call it like a local API:
#    POST http://localhost:8080/v1/chat/completions
#    Body: { "model": "Llama-3.2-3B", "messages": [...] }
```

This is the fastest way to get a working demo today. The downside: users need the MLC-LLM app installed alongside yours. Fine for a prototype/demo, not ideal for submission.

---

### Approach C — Fallback to Snapdragon 8 Elite Device (If Available)

If your lab has access to any of these devices from the supported device list, **AI Hub models work natively** and you can use the original Genie SDK + `.aimodel` approach:

| Device | Chipset | AI Hub LLM Support |
|---|---|---|
| Samsung Galaxy S25 / S25+ / S25 Ultra | Snapdragon 8 Elite | ✅ Full AI Hub support |
| Snapdragon 8 Elite QRD | Snapdragon 8 Elite | ✅ Full AI Hub support |
| Samsung Galaxy S24 / S24 Ultra | **Snapdragon 8 Gen 3** | ❌ No LLM support on AI Hub |
| HDK8650 MP | **Snapdragon 8 Gen 3** | ❌ No LLM support on AI Hub |

If you can borrow a Galaxy S25 or S25 Ultra from the lab, you can use the Genie SDK path from the original plan:
```bash
python -m qai_hub_models.models.llama_v3_2_3b_chat.export \
  --device "Samsung Galaxy S25" \
  --target-runtime qnn \
  --output-dir ~/llama_model/
```

Otherwise, stick with **Approach A (llama.cpp)** on the HDK8650.

---

### GenieWrapper.kt — Update for llama.cpp Backend

The `GenieWrapper.kt` file originally called the Genie SDK. Replace with an HTTP call to the llama.cpp server running on-device:

```kotlin
// GenieWrapper.kt — llama.cpp backend (for Snapdragon 8 Gen 3)
object GenieWrapper {
    private const val LLAMACPP_SERVER = "http://127.0.0.1:8080"

    suspend fun generate(systemPrompt: String, userMessage: String): String {
        val requestBody = JSONObject().apply {
            put("model", "llama-3.2-3b")
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user");   put("content", userMessage) })
            })
            put("max_tokens", 512)
            put("temperature", 0.7)
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("$LLAMACPP_SERVER/v1/chat/completions")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body!!.string())
                json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
        }
    }
}
```

To start the llama.cpp server on-device (run once at app startup via a background service):
```bash
# On device (called via adb or from your app using Runtime.exec):
/data/local/tmp/llama-cli \
  --server \
  -m /data/local/tmp/Llama-3.2-3B-Instruct-Q4_K_M.gguf \
  -ngl 99 \
  --host 127.0.0.1 \
  --port 8080 \
  --ctx-size 4096
```

---

### Verify LLM Works on Device

```bash
adb shell

# Check Vulkan support (needed for GPU inference)
ls /vendor/lib64/ | grep vulkan
# Expected: libvulkan.so (present on all 8 Gen 3 devices)

# Check available storage (model needs ~2.5 GB free)
df -h /data
# If less than 3 GB free, clear space first

# Quick API test once server is running:
curl http://127.0.0.1:8080/v1/models
# Should return: { "data": [{ "id": "llama-3.2-3b" ... }] }

exit
```

> **Risk Register Update:** Replace the "Genie SDK HTP won't load today" row with: "llama.cpp Vulkan not available on device — mitigation: fall back to `-ngl 0` (CPU-only, ~5 tok/s). Responses will be slower but still functional for demo. Alternatively, use Approach B (MLC-LLM app)."

---

## TODAY TASK 2 — Build the Basic App in Android Studio

### Gemini Prompt to Paste in Android Studio

Open Android Studio → click the **Gemini** icon (sparkle, right panel) → paste this exact prompt:

---

```
I'm building an AI tutor Android app for Remote Triggered Labs at IIIT Hyderabad. 
Create a complete Android project skeleton in Kotlin with the following:

PROJECT SETUP:
- Package name: ac.iiit.rtltutor
- Min SDK: API 29 (Android 10), Target SDK: API 34
- Language: Kotlin
- Use ViewBinding (not Compose) for all layouts
- Use Material3 components throughout
- Dark theme as default

ARCHITECTURE:
- MVVM pattern: ViewModel + LiveData for all screens
- Repository pattern for data access
- Single Activity with Navigation Component (nav_graph.xml)
- 8 fragments: LoginFragment, HomeFragment, ChatFragment, QuizFragment, 
  FlashcardFragment, RtlLiveViewFragment, ProgressFragment, SettingsFragment

DATA MODELS (create as data classes in models/ package):
- User(id: String, username: String, displayName: String, role: UserRole, passwordHash: String, keySalt: ByteArray, createdAt: Long)
- UserRole enum: STUDENT, ADMIN
- ChatMessage(id: String, userId: String, content: String, isFromAI: Boolean, bloomLevel: Int, kolbStage: String, timestamp: Long)
- ExperimentState(voltage: Double, current: Double, frequency: Double, timestamp: Long, rawJson: String)
- UserLearningProfile(userId: String, bloomMastery: Map<Int, Float>, weakTopics: List<String>, strongTopics: List<String>, sessionCount: Int)
- QuizQuestion(id: String, text: String, bloomLevel: Int, kolbStage: String, answer: String, hints: List<String>, difficulty: Int)

COLOR SYSTEM in colors.xml — use EXACTLY these values:
- bg_primary: #080C14
- bg_surface: #0E1521
- bg_elevated: #1A2438
- accent_cyan: #00D4FF
- accent_teal: #00B896
- accent_amber: #F5A623
- accent_violet: #8B5CF6
- text_primary: #F0F4FF
- text_secondary: #8899BB
- text_muted: #4A5568
- bloom_1: #4A5568, bloom_2: #2563EB, bloom_3: #059669
- bloom_4: #D97706, bloom_5: #DC2626, bloom_6: #7C3AED

TYPOGRAPHY in themes.xml:
- Download and add these Google Fonts to res/font/: space_grotesk (headlines), inter (body), jetbrains_mono (data/code)
- Apply Space Grotesk to all TextAppearance.MaterialComponents.Headline styles
- Apply Inter to body styles
- Apply JetBrains Mono to a custom textAppearanceMonospace style

SCREENS TO BUILD TODAY (basic shells with correct colors and layout):

1. LoginFragment layout (fragment_login.xml):
- Background: bg_primary
- Centered card (bg_surface, 16dp corner radius, no shadow)
- App name "RTL Tutor" in Space Grotesk Bold 28sp, text_primary
- Subtle oscilloscope-wave SVG shape as background watermark (very low opacity, color accent_cyan)
- Username TextInputLayout with cyan focus ring
- Password TextInputLayout (passwordToggle enabled) with cyan focus ring
- "Sign In" button: filled, accent_cyan background, bg_primary text, full width, 52dp height, 12dp corner radius
- "Create Account" text button below in text_secondary color
- Error text in bloom_5 color

2. HomeFragment layout (fragment_home.xml):
- Background: bg_primary
- Top: greeting text "Hello, [Name]" in Space Grotesk 22sp
- RTL Status Card (bg_surface, 12dp radius): left side has pulsing green dot when connected, title "RTL Lab", subtitle "Simulator Active" / "Connected" / "Offline"
- Today's Plan card (bg_surface): shows current Kolb stage as colored pill
- Weak Topics card (bg_surface): horizontal scroll of amber chips  
- Streak counter row: flame icon + number in accent_amber
- Bottom navigation bar with 5 tabs: Home, Chat, Quiz, Flashcards, Progress (use icons from Material Icons)

3. ChatFragment layout (fragment_chat.xml):
- Background: bg_primary
- Top bar: Kolb stage pill (color-coded: teal=DO, amber=REVIEW, blue=LEARN, violet=TRY) + Bloom level indicator
- RecyclerView for messages (no dividers)
- AI message item (item_message_ai.xml): left-aligned, bg_surface card, 1dp left border in accent_cyan, small waveform icon, message text in Inter, Bloom badge chip at bottom-right
- User message item (item_message_user.xml): right-aligned, bg_elevated card, no border
- Bottom bar: TextInputEditText (bg_elevated, rounded 24dp), Send button (accent_cyan icon), Voice button (microphone icon, accent_teal)
- "AI Thinking..." collapsible section above AI messages: monospace text in accent_violet on #0A0614 background

DEPENDENCIES to add in app/build.gradle:
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'com.google.code.gson:gson:2.10.1'
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
implementation 'org.mindrot:jbcrypt:0.4'
implementation 'androidx.security:security-crypto:1.1.0-alpha06'
implementation 'androidx.navigation:navigation-fragment-ktx:2.7.7'
implementation 'androidx.navigation:navigation-ui-ktx:2.7.7'
implementation 'com.google.android.material:material:1.12.0'

STUB CLASSES to create (empty implementations, just the class + method signatures):
- GenieWrapper.kt: loadModel(path: String), generate(system: String, user: String, onToken: (String)->Unit), unload()
- RTLConnector.kt: connect(url: String, token: String), disconnect(), latestState: ExperimentState
- RTLSimulator.kt: start(), stop() — returns RC circuit data every 1s
- EncryptionManager.kt: deriveKey(password, salt), encrypt(data, key), decrypt(data, key)
- UserRepository.kt: createUser(), login(), getAllUsers()
- BloomsTagger.kt: tag(text: String): Int
- KolbStageManager.kt: currentStage, onExperimentStart(), onExperimentEnd(), onTheoryTriggered()
- SocraticEngine.kt: generateGuide(question: String, profile: UserLearningProfile): String

Make sure the app compiles and runs on first launch showing the Login screen with the dark UI.
```

---

After Gemini generates the code, do these manual fixes:
1. Add `android:theme="@style/Theme.RTLTutor"` to `AndroidManifest.xml` application tag
2. Set `darkMode = true` in your Application class
3. Verify the app builds and launches on the QIDK device (`Run → Run 'app'`)

---

## TODAY TASK 3 — Wire LLM into the App (Verify End-to-End)

Add this to your project. This is the only file you write manually today.

### GenieWrapper.kt (put in `app/src/main/java/ac/iiit/rtltutor/ai/`)

```kotlin
package ac.iiit.rtltutor.ai

import android.content.Context
import android.util.Log

/**
 * Single interface to the on-device Llama 3.2 3B model via Qualcomm Genie SDK.
 * 
 * TODAY: Runs in mock mode (echoes prompt back) so the UI works before the SDK is wired.
 * WEEK 1: Replace mock with real GenieModel calls once Genie SDK AAR is added to project.
 */
class GenieWrapper(private val context: Context) {

    private var isLoaded = false
    private val TAG = "GenieWrapper"

    // === TODAY: MOCK MODE ===
    // This lets the chat UI work without the Genie SDK.
    // Replace everything inside generate() with real SDK calls in Week 1.

    fun loadModel(modelPath: String) {
        // TODO Week 1: Replace with GenieModel(context, config).load()
        Log.d(TAG, "Mock: model would load from $modelPath")
        isLoaded = true
    }

    fun generate(
        systemPrompt: String,
        userMessage: String,
        onToken: (String) -> Unit,
        onComplete: () -> Unit = {}
    ) {
        if (!isLoaded) {
            onToken("Model not loaded. Check model path.")
            onComplete()
            return
        }

        // TODO Week 1: Replace with real async inference via GenieModel.generateAsync()
        // Mock: simulate streaming by splitting a canned Socratic response
        val mockResponse = buildSocraticMockResponse(userMessage)
        Thread {
            mockResponse.split(" ").forEach { word ->
                Thread.sleep(80)  // simulate token streaming delay
                onToken("$word ")
            }
            onComplete()
        }.start()
    }

    fun generateSync(systemPrompt: String, userMessage: String): String {
        // TODO Week 1: Replace with blocking GenieModel call
        return buildSocraticMockResponse(userMessage)
    }

    fun unload() {
        // TODO Week 1: genieModel?.unload()
        isLoaded = false
    }

    private fun buildSocraticMockResponse(question: String): String {
        return when {
            question.contains("what", ignoreCase = true) || 
            question.contains("explain", ignoreCase = true) ->
                "That's a great question. Before I explain, let me ask you this — " +
                "what do you already know about this concept? " +
                "What does your intuition tell you? [Bloom:2]"
            question.contains("why", ignoreCase = true) ->
                "Interesting observation. Let's think about this together. " +
                "What did you expect to see, and how does that differ from what you observed? [Bloom:4]"
            question.contains("how", ignoreCase = true) ->
                "Good thinking. Can you break down what you think is happening step by step? " +
                "Start with the inputs — what are you controlling? [Bloom:3]"
            else ->
                "Let me guide you rather than answer directly. " +
                "What information do you have available right now to help solve this? [Bloom:2]"
        }
    }

    // === WEEK 1 REAL IMPLEMENTATION (uncomment when Genie SDK is in build.gradle) ===
    //
    // private var genieModel: GenieModel? = null
    //
    // fun loadModelReal(modelPath: String) {
    //     val config = GenieConfig.Builder()
    //         .setModelPath(modelPath)
    //         .setBackend(GenieBackend.HTP)   // <-- CRITICAL: must be HTP not CPU
    //         .setMaxTokens(512)
    //         .build()
    //     genieModel = GenieModel(context, config)
    //     genieModel?.load()
    //     isLoaded = true
    // }
    //
    // fun generateReal(system: String, user: String, onToken: (String)->Unit, onComplete: ()->Unit) {
    //     val prompt = buildLlamaPrompt(system, user)
    //     genieModel?.generateAsync(prompt) { token ->
    //         onToken(token)
    //         if (token == "<|eot_id|>") onComplete()
    //     }
    // }
    //
    // private fun buildLlamaPrompt(system: String, user: String) =
    //     "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n$system" +
    //     "<|eot_id|><|start_header_id|>user<|end_header_id|>\n$user" +
    //     "<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n"
}
```

### ChatViewModel.kt (connect to the wrapper)

```kotlin
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val genieWrapper = GenieWrapper(application)
    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    val isTyping = MutableLiveData(false)
    val currentKolbStage = MutableLiveData("DO")
    val currentBloomLevel = MutableLiveData(1)

    init {
        // Load model from device storage
        // In Week 1: use real model path after adb push
        // For today: mock mode, path doesn't matter
        genieWrapper.loadModel("/data/local/tmp/llama_v3_2_3b_quantized.aimodel")
    }

    fun sendMessage(userText: String) {
        // Add user message to list
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            userId = "current_user",
            content = userText,
            isFromAI = false,
            bloomLevel = 0,
            kolbStage = currentKolbStage.value ?: "DO",
            timestamp = System.currentTimeMillis()
        )
        addMessage(userMsg)
        isTyping.postValue(true)

        // Build AI response message (starts empty, fills via streaming)
        val aiMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            userId = "ai",
            content = "",
            isFromAI = true,
            bloomLevel = currentBloomLevel.value ?: 1,
            kolbStage = currentKolbStage.value ?: "DO",
            timestamp = System.currentTimeMillis()
        )
        addMessage(aiMsg)

        var accumulated = ""
        genieWrapper.generate(
            systemPrompt = buildSystemPrompt(),
            userMessage = userText,
            onToken = { token ->
                accumulated += token
                // Update the last message in the list with accumulated tokens
                val updated = _messages.value?.toMutableList() ?: return@generate
                updated[updated.lastIndex] = aiMsg.copy(content = accumulated)
                _messages.postValue(updated)
            },
            onComplete = {
                isTyping.postValue(false)
                // Extract Bloom level from [Bloom:N] tag at end of response
                val bloomMatch = Regex("\\[Bloom:(\\d)\\]").find(accumulated)
                bloomMatch?.groupValues?.get(1)?.toIntOrNull()?.let {
                    currentBloomLevel.postValue(it)
                }
            }
        )
    }

    private fun addMessage(msg: ChatMessage) {
        val current = _messages.value?.toMutableList() ?: mutableListOf()
        current.add(msg)
        _messages.postValue(current)
    }

    private fun buildSystemPrompt(): String {
        return """
            You are RTL Tutor, an expert AI tutor for the IIITH Remote Triggered Lab.
            You are patient, encouraging, and Socratic.
            
            CORE RULES:
            1. NEVER give a direct answer on the first question. Always ask 1-2 guiding questions first.
            2. NEVER make up scientific values or constants.
            3. Every response must end with [Bloom:N] where N is 1-6.
            4. Adapt your question difficulty to the student's demonstrated level.
            
            CURRENT STATE:
            Kolb Stage: ${currentKolbStage.value}
            Bloom Target: ${currentBloomLevel.value}
        """.trimIndent()
    }

    override fun onCleared() {
        super.onCleared()
        genieWrapper.unload()
    }
}
```

### End-of-Today Verification Checklist

Run the app on the QIDK device. You should be able to:
- [ ] App launches → shows Login screen with dark UI (#080C14 background)
- [ ] Type any message in Chat → AI responds with a Socratic guiding question
- [ ] Response streams in word by word (not all at once)
- [ ] Response ends with a [Bloom:N] tag
- [ ] No crashes

If these pass, today is done. The LLM is mocked for now — Week 1 replaces the mock with real Genie SDK inference.

---

## TODAY TASK 4 — Final Proposed Solution (What This App Will Be)

### Problem
Students doing Remote Triggered Lab experiments at IIITH have no one to ask when confused. 40–60% of sessions end in dropout when a student hits a conceptual block. Generic AI chatbots hallucinate scientific facts and send data to external servers.

### Our Solution

An **on-device AI tutor** running entirely offline on the Snapdragon 8 Gen 3 NPU that:

**Knows the student** — multi-user login with encrypted per-user data. The AI tracks what each student understands and what they struggle with across sessions, and adapts every conversation to that individual.

**Teaches like a great teacher** — pure Socratic method. Never gives direct answers. Asks guiding questions calibrated to the student's demonstrated Bloom's Taxonomy level. If a student is operating at Level 2 (Understand), the AI asks Level 3 questions to push them forward. As they master concepts, questions get harder automatically.

**Sees the actual experiment** — connects to the RTL live data stream. When voltage spikes at t=4.2s, the AI asks "What do you think caused that spike at 4.2 seconds?" because it genuinely knows the voltage spiked at 4.2 seconds.

**Cannot hallucinate facts** — three-layer fact-check: RAG grounding, reference JSON injection with all scientific constants for the experiment, and a validation pass on every response containing a number.

**Lets students create** — at Bloom's Level 6 (Create), the AI generates a complete interactive HTML experiment with sliders and Chart.js visualizations that the student can manipulate to test their own hypotheses. No one else does this.

**Gets smarter over time** — fine-tuned on Socratic dialogue specific to RTL experiments, making the guiding questions sharper than a base model can achieve with prompting alone.

**Never shares data** — zero bytes leave the device. All chat history, quiz results, and user profiles are AES-256 encrypted on-device. Only the RTL intranet connection exists.

### Feature List

| Priority | Feature |
|---|---|
| P0 | Multi-user login with Admin role |
| P0 | AES-256 encrypted per-user storage |
| P0 | Socratic cross-questioning (adaptive to student level) |
| P0 | Bloom's Taxonomy tagging on every response |
| P0 | Kolb's Experiential Cycle stage manager |
| P0 | 3-layer fact-check (RAG + JSON injection + validation) |
| P0 | RTL live data stream → AI context injection |
| P0 | Chat history (persistent, encrypted, searchable) |
| P0 | Adaptive quiz with SM-2 spaced repetition |
| P0 | AI learns from previous chats (UserLearningProfile) |
| P0 | Interactive UI (modern dark design, streaming AI text) |
| P0 | Fine-tuned LLM on Socratic dialogue + Bloom's tagging |
| P1 | Bloom Level 6: AI-generated interactive HTML experiments |
| P1 | Lab report auto-generator from session data |
| P1 | Bloom's radar chart on Progress screen |
| P1 | Voice input/output (offline STT/TTS) |
| P2 | PDF/text export of lab reports |
| P2 | Privacy Dashboard screen |
| P2 | Streak gamification and badges |

---

# ═══════════════════════════════════════════
# 8-WEEK TIMELINE (Starts Sep 3, 2026)
# ═══════════════════════════════════════════

```
WEEK  1: Sep  3 – Sep  9   │ Foundation: real LLM, auth, encryption
WEEK  2: Sep 10 – Sep 16   │ ◀ DELIVERABLE 1: Working app with LLM + multiuser
WEEK  3: Sep 17 – Sep 23   │ AI engine: Bloom's, Kolb's, Socratic, fact-check
WEEK  4: Sep 24 – Sep 30   │ ◀ DELIVERABLE 2: Full pedagogical engine + RTL stub
WEEK  5: Oct  1 – Oct  7   │ RTL live data + adaptive quiz + fine-tuning dataset
WEEK  6: Oct  8 – Oct 14   │ ◀ DELIVERABLE 3: RTL connected + quiz + fine-tune running
WEEK  7: Oct 15 – Oct 21   │ Fine-tune eval + interactive experiments + polish
WEEK  8: Oct 22 – Oct 28   │ ◀ DELIVERABLE 4 (FINAL): Everything integrated + submitted
```

*Submission deadline is Nov 10 — Weeks 8+ are buffer. Aim to be done by Oct 28.*

---

## WEEK 1 · Sep 3–9 · Real LLM + Auth + Encryption

### Goal
Replace today's mock LLM with real Genie SDK inference. Build working login with real encryption.

### Tasks
**LLM (do first — highest risk)**
- [ ] Add Genie SDK AAR to `app/libs/` and wire into `build.gradle`
- [ ] Uncomment the real implementation in `GenieWrapper.kt` (see comments in the file)
- [ ] Call `loadModel("/data/local/tmp/llama_v3_2_3b_quantized.aimodel")` with `GenieBackend.HTP`
- [ ] Verify first-token latency < 1.5s and full response < 5s
- [ ] If HTP fails: fall back to `GenieBackend.CPU` temporarily (slower but works) and file a bug with Qualcomm QDN

**Authentication**
- [ ] Implement `UserRepository.kt` fully: `createUser()`, `login()`, `getAllUsers()`
- [ ] Store user registry as JSON in `context.filesDir/users_registry.json`
- [ ] Implement `EncryptionManager.kt`: AES-256-GCM + PBKDF2 key derivation (100,000 iterations)
- [ ] Wire `LoginFragment` → `UserRepository.login()` → navigate to `HomeFragment` on success
- [ ] Wire Registration flow → `UserRepository.createUser()`
- [ ] Admin account: create a hardcoded admin user on first launch if none exists

**Encryption**
- [ ] Encrypt all chat messages before writing to disk (use `EncryptionManager.encrypt()`)
- [ ] Decrypt on load; verify: open the .enc file in a text editor — must be unreadable
- [ ] Admin flow: Admin login shows all users in a list; tapping a user shows their session count and Bloom mastery summary

**RTL Simulator**
- [ ] Implement `RTLSimulator.kt` fully: RC circuit model, updates every 1s
- [ ] Wire to `HomeFragment`: RTL status card shows "Simulator Active" with pulsing dot

**Infrastructure**
- [ ] Set up shared Git repo: `main` / `develop` / `feature/*` branch strategy
- [ ] CI: GitHub Actions automated build check on every push
- [ ] Scout GPU compute: departmental cluster, Colab Pro, or cloud credits for fine-tuning Week 5

### Done Criteria (Week 1)
- Real LLM responds on device via Genie SDK HTP backend
- User can register, log in, and their session is stored encrypted
- Admin can log in and see all users

---

## WEEK 2 · Sep 10–16 · Polish + Chat History + D1

### Goal
Make everything from Week 1 production-quality. This is the first deliverable demo.

### Tasks
- [ ] Chat history: save each session as encrypted JSON file (`users/{id}/sessions/{timestamp}.enc`)
- [ ] Session list screen: tap any past session to reload it in Chat
- [ ] Search across chat history by keyword (decrypt and search in memory)
- [ ] `UserLearningProfile`: create data class, save/load encrypted per user
- [ ] After each session: use LLM to extract 3 insights about the student → update profile
- [ ] Wire profile into system prompt: AI mentions student's known struggles in responses
- [ ] Complete all 3 Chat screen layouts: AI message, user message, streaming state
- [ ] Kolb badge in Chat top bar (static DO for now, changes in Week 3)
- [ ] Complete `HomeFragment`: working RTL status card, streak counter (starts at 1 on first login), weak topics chips (placeholder until quiz data exists)
- [ ] `SettingsFragment`: dark/light toggle, font size slider, clear data button

### Done Criteria — Deliverable 1 (End of Week 2)

**Demo these in order:**
1. Fresh install → register two student accounts (Student A, Student B) + admin
2. Student A chats with AI → AI asks Socratic questions → [Bloom:N] tags visible
3. Log out Student A → log in Student B → Student B cannot see Student A's chat
4. Log in as Admin → see both users in the admin list with session counts
5. Log in as Student A again → previous chat history is there, still readable
6. Open the `.enc` file on your laptop — show it's unreadable hex

**Numbers to hit:**
- LLM responds in < 5s on HTP
- Zero plaintext chat data on disk (verify with hex editor)
- All screens load without crash

---

## WEEK 3 · Sep 17–23 · Bloom's + Kolb's + Socratic Engine + Fact-Check

### Goal
Build the complete AI pedagogical brain.

### Tasks
**BloomsTagger**
- [ ] Rule-based classifier (L1–L3): keyword matching ("what is", "define", "list" → L1; "explain", "describe", "summarize" → L2; "calculate", "apply", "use" → L3)
- [ ] Heuristic classifier (L4–L6): sentence complexity + question word + length
- [ ] Log every input/output pair to `bloom_tag_log.jsonl` (auto-runs forever, free training data)
- [ ] Wire into inference pipeline: BloomsTagger tags user question → sets `BLOOM_TARGET` in system prompt

**KolbStageManager**
- [ ] 4-state machine: DO → REVIEW → LEARN → TRY, with defined triggers
- [ ] Wire to UI: Kolb badge in Chat updates live (teal=DO, amber=REVIEW, blue=LEARN, violet=TRY)
- [ ] Wire system prompt: AI behavior changes per stage (see system prompt template below)

**SocraticEngine**
- [ ] Intercept every user question before inference
- [ ] System prompt enforces: first response is always 1–2 guiding questions
- [ ] Hint stages: "I don't know" response 1 → guiding question; response 2 → partial answer; response 3 → full answer
- [ ] Cross-questioning: read `UserLearningProfile.bloomMastery` → ask at their level +1
- [ ] Misconception detector: if user's answer contradicts a known constant → "Interesting — but consider this: what does [constant] tell us?"

**3-Layer Fact-Check**
- [ ] Layer 1 — ContextWindowManager: relevance threshold 0.45; below threshold → "I don't have verified information on this topic in my reference material."
- [ ] Layer 2 — ConstantsInjector: load `scientific_constants.json`, keyword-match user query, inject matching entries into system prompt as "Reference values: ..."
- [ ] Layer 3 — ValidationPrompt: if AI response contains a digit → second LLM call to check against constants → if FAIL, append "Note: verify these values against your lab manual."

**System Prompt Template (wire this in Week 3)**
```
You are RTL Tutor at IIITH. Patient, encouraging, Socratic.

STUDENT PROFILE:
Name: {{USER_NAME}}
Bloom Mastery: L1={{B1}}% L2={{B2}}% L3={{B3}}% L4={{B4}}% L5={{B5}}% L6={{B6}}%
Struggles with: {{WEAK_TOPICS}}
Strong in: {{STRONG_TOPICS}}

CURRENT CONTEXT:
Kolb Stage: {{KOLB_STAGE}}
Bloom Target (ask at this level): {{BLOOM_TARGET}}
Experiment: {{EXPERIMENT_NAME}}
Live Data: {{EXPERIMENT_STATE_JSON}}

REFERENCE CONSTANTS: {{INJECTED_CONSTANTS}}

RULES:
1. NEVER answer directly. Ask 1-2 guiding questions first.
2. NEVER invent numbers. Only use values from REFERENCE CONSTANTS.
3. If context doesn't contain the answer: say exactly "I don't have verified information on this topic in my reference material."
4. Every response ends with [Bloom:N].
5. Ask questions at BLOOM_TARGET level — not easier, not harder.

KOLB BEHAVIOR:
DO → Stay quiet unless student speaks; ask "What are you observing?"
REVIEW → Ask "What surprised you?" "What was different from what you expected?"
LEARN → Explain theory using reference material; connect to observed data
TRY → Generate hypothesis: "What do you predict if {{VARIABLE}} changes to {{VALUE}}?"

SOCRATIC HINTS:
Hint 1 (student says "I don't know" once): Ask a narrower guiding question
Hint 2 (twice): Give a partial answer with a gap: "The voltage is [value] — what does that suggest about...?"
Hint 3 (three times): Give the full answer with explanation
```

### Done Criteria (Week 3)
- BloomsTagger correctly tags 80%+ of 20 test questions
- Kolb badge updates live through all 4 stages in simulated session
- SocraticEngine NEVER gives a direct answer on first turn
- Fact-check blocks 5 out-of-context test queries with exact fallback phrase
- ValidationPrompt catches 3 deliberate numerical errors

---

## WEEK 4 · Sep 24–30 · Adaptive Quiz + RTL Meeting + D2

### Goal
Build the adaptive quiz. Meet RTL coordinator. **Deliverable 2**.

### Tasks
**RTL**
- [ ] Meet RTL lab coordinator this week — ask: URL, protocol, auth, JSON schema, available experiments
- [ ] Document in `docs/rtl_api.md`
- [ ] Choose one experiment (criteria: numeric sensor data, covers all 6 Bloom levels, Kolb cycle applies)
- [ ] Update `scientific_constants.json` with real values from chosen experiment's lab manual

**Adaptive Quiz**
- [ ] `AdaptiveQuizEngine`: SM-2 spaced repetition (same algorithm as Anki)
  - On correct: ease factor increases, next review interval multiplied
  - On wrong: reset interval, decrease ease factor
  - Bloom level: up after 3 consecutive correct; down after 2 consecutive wrong
- [ ] Build `question_bank.json`: 60+ questions for chosen experiment, each with `bloom_level`, `kolb_stage`, `answer`, `hints[3]`, `difficulty`
- [ ] Wire question bank into quiz: curated questions first, LLM-generated as fallback
- [ ] `WrongAnswerExplainer`: wrong answer → SocraticEngine guiding question (not "the answer is X")
- [ ] Quiz session persistence: save state to encrypted JSON on app close
- [ ] Build `QuizFragment`: Bloom badge (changes color per level), difficulty bar, hint button, next review time shown per question

**Fine-Tuning Prep**
- [ ] Start `fine_tune/socratic_pairs.jsonl`: write 150 hand-crafted (question → ideal 2-3 turn Socratic dialogue) pairs for your chosen experiment

### Done Criteria — Deliverable 2 (End of Week 4)

**Demo these in order:**
1. Show Chat with live Kolb badge transitioning through stages during a simulated session
2. Show fact-checker blocking a made-up voltage value with the fallback phrase
3. Show SocraticEngine: ask a direct question → AI guides with questions, not answers
4. Show Quiz: answer 3 questions correctly → Bloom badge jumps one level (color change visible)
5. Show SM-2: easy question gets a 7-day next review, hard question gets 1-day
6. Show `docs/rtl_api.md` with RTL API documentation (or fallback plan if coordinator not available yet)

---

## WEEK 5 · Oct 1–7 · RTL Live Connection + Fine-Tuning Dataset

### Goal
Replace the simulator with real RTL data. Lock in the fine-tuning dataset.

### Tasks
**RTL Integration**
- [ ] Implement real `RTLConnector.kt` (WebSocket via OkHttp, or REST polling — whichever the RTL uses)
- [ ] `ExperimentStateParser`: deserialize RTL JSON → `ExperimentState` (use flexible `Map` parsing)
- [ ] Wire `ExperimentState` into AI system prompt: every inference call includes live readings
- [ ] Wire `KolbStageManager` to RTL events: experiment start → DO, end → REVIEW
- [ ] Build `RtlLiveViewFragment`: MPAndroidChart line chart (cyan line, no gridlines), gauge cards (JetBrains Mono numbers), connection indicator (pulsing dot)
- [ ] Handle connection drops: reconnect every 3s, show "Reconnecting..." in UI
- [ ] Test: AI correctly references actual sensor values in 5 test queries

**Fine-Tuning Dataset**
- [ ] Expand `socratic_pairs.jsonl` to 400+ pairs (split work across team)
- [ ] Second team member reviews pairs for consistency and quality
- [ ] Combine with `bloom_tag_log.jsonl` (auto-generated since Week 3)
- [ ] Finalize dataset: 500+ total pairs, split 90/10 train/eval, write data card

**Additional**
- [ ] `ExperimentGenerator.kt` skeleton: LLM generates interactive HTML experiment for Bloom L6
- [ ] `LabReportGenerator.kt` skeleton: structured report from session data

### Done Criteria (Week 5)
- App connects to real RTL (or high-quality simulator if API not yet approved)
- Live sensor values appear in RTL Live View with < 2s latency
- AI says "Your voltage is currently X.Xv — what does that suggest?" with real values
- 500+ fine-tuning pairs finalized

---

## WEEK 6 · Oct 8–14 · Fine-Tuning Run + Voice + D3

### Goal
Run the fine-tuning job. Add voice. **Deliverable 3**.

### Tasks
**Fine-Tuning (off-device — run on cluster/Colab/cloud)**
- [ ] Run QLoRA training job:
  - Base: Llama 3.2 3B Instruct (same weights as on-device model)
  - Method: QLoRA, 4-bit base, LoRA rank 8–16
  - Dataset: all files from Week 5
  - Target: ~4–8 hours on T4 GPU, ~1–2 hours on A100
  ```python
  # Colab setup (paste in a Colab Pro notebook)
  !pip install transformers peft bitsandbytes datasets accelerate trl
  
  from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig
  from peft import LoraConfig, get_peft_model
  from trl import SFTTrainer, SFTConfig
  import torch, json
  
  model_id = "meta-llama/Llama-3.2-3B-Instruct"
  
  bnb_config = BitsAndBytesConfig(
      load_in_4bit=True,
      bnb_4bit_quant_type="nf4",
      bnb_4bit_compute_dtype=torch.float16
  )
  
  model = AutoModelForCausalLM.from_pretrained(model_id, quantization_config=bnb_config)
  tokenizer = AutoTokenizer.from_pretrained(model_id)
  
  lora_config = LoraConfig(
      r=16,                          # rank
      lora_alpha=32,
      target_modules=["q_proj", "v_proj", "k_proj", "o_proj"],
      lora_dropout=0.05,
      bias="none",
      task_type="CAUSAL_LM"
  )
  
  model = get_peft_model(model, lora_config)
  model.print_trainable_parameters()
  # Expected: ~0.5% of parameters — that's the LoRA efficiency
  
  # Load your dataset from socratic_pairs.jsonl
  # Format each entry as: {"text": "<|system|>...<|user|>...<|assistant|>..."}
  
  trainer = SFTTrainer(
      model=model,
      train_dataset=train_dataset,
      eval_dataset=eval_dataset,
      args=SFTConfig(
          output_dir="./rtl_tutor_lora",
          num_train_epochs=3,
          per_device_train_batch_size=4,
          gradient_accumulation_steps=4,
          warmup_steps=100,
          learning_rate=2e-4,
          evaluation_strategy="epoch",
          logging_steps=50,
      ),
  )
  trainer.train()
  trainer.save_model("./rtl_tutor_lora_final")
  ```
- [ ] Export adapter weights as separate LoRA adapter (not merged) — smaller file, easier to test

**Voice**
- [ ] `VoiceModule.kt`: Android `SpeechRecognizer` (offline STT) + `TextToSpeech` (offline TTS)
- [ ] STT confidence threshold 0.7: show text for confirmation if below
- [ ] Hold-to-speak button in Chat: red while recording, sends on release
- [ ] TTS reads AI responses aloud (toggleable in Settings)
- [ ] Test: 15 phrases, > 80% accuracy in quiet room; full voice loop < 5s

**Other**
- [ ] `FlashcardFragment`: flip animation (3D Y-axis rotation), SM-2 self-rating (Easy/Medium/Hard), due-today counter
- [ ] Streak gamification: daily counter, milestone badges (7 days, first Bloom L6, first lab complete)

### Done Criteria — Deliverable 3 (End of Week 6)

**Demo these in order:**
1. Show RTL Live View: live sensor data updating, AI references actual values in chat
2. Show adaptive quiz: Bloom level adjusts mid-quiz (demonstrate 3 correct → level up)
3. Show voice: speak a question → AI responds aloud
4. Show fine-tuning: training job has completed (show loss curve from Colab)
5. Show flashcards: flip animation + spaced repetition schedule updating on rating
6. Show the fine-tune adapter file ready for evaluation

---

## WEEK 7 · Oct 15–21 · Fine-Tune Evaluation + Interactive Experiments + Full Polish

### Goal
Ship the fine-tuned model if it wins. Build interactive experiment generator. Polish all 8 screens.

### Tasks
**Fine-Tune Evaluation**
- [ ] Convert LoRA adapter for QIDK: merge into base model or load as separate adapter via Genie SDK
- [ ] Deploy to QIDK alongside base model
- [ ] Blind pairwise evaluation (all 4 team members, independently):
  - Run 20 held-out questions through both models
  - Rate each response: (A) did it tag Bloom level correctly? (B) did it ask a guiding question instead of answering directly?
  - Count wins for each model
- [ ] If fine-tuned wins clearly on BOTH metrics AND passes < 3s latency test → ship it (swap in `GenieWrapper`)
- [ ] If not → keep base model, document result in submission

**Interactive Experiment Generator (Bloom Level 6)**
- [ ] `ExperimentGenerator`: AI generates self-contained HTML with:
  - 2+ sliders (e.g., voltage, resistance)
  - Chart.js real-time graph updating on slider change
  - "Observation" box that auto-updates with calculated insight
  - Dark theme matching app colors
- [ ] Display in `WebView` (full-screen bottom sheet)
- [ ] Trigger: when AI tags a response [Bloom:6], show "Generate Experiment" button

**Lab Report**
- [ ] `LabReportGenerator`: structured report sections (Objective, Apparatus, Observations auto-filled from RTL data, Theory, Analysis, Conclusion)
- [ ] Export as plain text (share via Android share sheet)

**Progress Screen**
- [ ] Bloom's radar chart: MPAndroidChart, 6 axes, cyan fill at 30% opacity
- [ ] Per-topic accuracy bars
- [ ] Streak calendar (GitHub-style grid, cyan shades)

**Full UI Polish**
- [ ] Complete redesign of all remaining screens: Quiz, Flashcard, Lab Report, Settings
- [ ] Accessibility pass: TalkBack, 48dp touch targets, content descriptions
- [ ] Network audit: verify zero non-RTL network calls in release build (Android Network Profiler)
- [ ] Privacy Dashboard: checklist of all on-device data, RTL connection status

### Done Criteria (Week 7)
- Fine-tune decision documented with numbers
- Bloom Level 6 generates a working interactive HTML experiment
- All 8 screens match design system with no placeholder text
- Lab report generated from real session data
- Zero non-RTL network calls confirmed

---

## WEEK 8 · Oct 22–28 · Testing + Demo + Final Submission · D4

### Goal
All P0 features pass. Submitted. **Deliverable 4 (Final)**.

### Tasks
- [ ] Full end-to-end test: connect RTL → complete Kolb cycle → generate lab report → export
- [ ] Test every P0 acceptance criterion from the list above (use as a checklist)
- [ ] Fix all critical (crash) and high (feature broken) bugs found
- [ ] Performance profiling: document inference latency, RTL latency, chart render time, voice round-trip
- [ ] Tag `v1.0.0` release in Git; generate signed APK; test signed APK on QIDK
- [ ] Write documentation: README.md, feature guide (one page per P0 feature), architecture overview
- [ ] Record demo video (3–5 minutes) — see script below
- [ ] Final submission document with problem statement, motivation, features, architecture, roadmap, demo link, team contributions
- [ ] All 4 team members review and sign off before submitting

### Demo Video Script (3–5 minutes)
1. (0:00) App launches on QIDK device — show dark UI, show it's running on actual hardware
2. (0:15) Register Student A — show account created
3. (0:30) Student A starts experiment — RTL Live View shows sensor data updating
4. (0:45) Kolb badge shows DO → student types a question → AI asks back (Socratic mode visible)
5. (1:00) Show Bloom badge changing as conversation deepens
6. (1:20) Kolb transitions to REVIEW → AI asks reflective questions
7. (1:45) Show AI referencing actual sensor values from the live experiment
8. (2:00) Move to Quiz → answer 3 questions correctly → Bloom level jumps (color change)
9. (2:20) Show voice: hold button, speak question, AI responds aloud
10. (2:45) Show Lab Report: auto-generated from session with real RTL values
11. (3:00) Show Progress screen: Bloom radar chart with real data
12. (3:15) Log in as Admin → see Student A's data and session stats
13. (3:30) Open .enc file on laptop → show it's unreadable

---

## Complete P0 Acceptance Checklist (Final Submission Gate)

**Multiuser & Security**
- [ ] Student A's encrypted files cannot be read by Student B (verify in file system)
- [ ] Admin login shows all users with session counts and Bloom mastery
- [ ] Chat history .enc files are unreadable hex without the user's password

**AI Pedagogy**
- [ ] AI NEVER gives a direct answer on the first turn — always asks guiding questions
- [ ] AI asks harder questions to students with higher BloomMastery (demonstrate with two different profiles)
- [ ] Every AI response and quiz question ends with [Bloom:N] tag
- [ ] Kolb stage badge transitions through all 4 stages in a live session
- [ ] Hint stages work: 3 consecutive "I don't know" responses → progressively more help

**Fact-Check**
- [ ] Out-of-context query returns exact fallback phrase (not a guess)
- [ ] ValidationPrompt flags a deliberately wrong number in a test response

**RTL Integration**
- [ ] Live sensor data appears in RTL Live View with < 2s latency
- [ ] AI correctly references actual sensor values in chat

**Quiz & Learning**
- [ ] Bloom level in quiz adjusts after 3 correct or 2 wrong consecutive answers
- [ ] SM-2 generates plausible review intervals (1 day easy, 5+ days medium/hard)
- [ ] Second session with same user: AI acknowledges topics from first session

**UI & Other**
- [ ] Bloom Level 6 generates a working interactive HTML experiment with sliders
- [ ] Lab report generated from real session data with RTL readings
- [ ] All 8 screens consistent with design system, no placeholder text
- [ ] Voice (P1): STT > 80% accuracy, TTS reads AI responses, full loop < 5s

---

## Risk Register

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| llama.cpp Vulkan not available / slow on 8 Gen 3 | Medium | High | Remove `-ngl 99` flag to fall back to CPU-only (~5 tok/s, slower but functional); alternatively use MLC-LLM app (Approach B) which handles GPU detection automatically; use mock mode in GenieWrapper for UI work while fixing |
| AI Hub LLM unavailable for 8 Gen 3 chipset | Confirmed | High | **Already mitigated** — switched to llama.cpp + GGUF path (Approach A); if a Snapdragon 8 Elite device (Galaxy S25) is available in lab, use Approach C (original Genie SDK path) |
| RTL API unavailable by Week 5 | Medium | High | Simulator has identical architecture; document as "pending API access" |
| Two-call inference > 3s | Medium | Medium | Profile in Week 3; make ValidationPrompt async |
| Fine-tuning compute unavailable | Medium | Low | P2 feature; base model is P0 and fully functional |
| 8-week timeline slips | Medium | High | Feature lock after Week 4: no new P0/P1 features; P2 cut first |
| Voice STT unavailable on device build | Low | Low | Cut voice; text-only chat is still a full P0 submission |
| QIDK device failure | Low | Critical | Identify backup device in Week 1; emulator CPU fallback for all non-HTP code |
| Fine-tuned model regresses accuracy | Low | Low | Hard ship criterion: only ship if wins on BOTH Bloom-tag AND Socratic quality AND latency |

---

## How RTL Data Stream Connects to AI (Full Reference)

```
RTL Server  ──WebSocket──▶  RTLConnector.kt  ──▶  ExperimentState (data class)
                                                           │
                                                           ▼
                                              ContextWindowManager.kt
                                              (injects into system prompt)
                                                           │
                                                           ▼
                                              "Live Data: {voltage: 3.2V, 
                                               current: 0.14A, freq: 50Hz}"
                                                           │
                                              + UserLearningProfile (student history)
                                              + scientific_constants.json (fact-check)
                                              + Bloom target + Kolb stage
                                                           │
                                                           ▼
                                              GenieWrapper.generate(systemPrompt, userMsg)
                                                           │
                                                           ▼
                                              "Your voltage is 3.2V right now.
                                               You expected 5V — what do you think
                                               is causing that 1.8V drop? [Bloom:4]"
```

The AI doesn't have eyes. It knows about the experiment because your code puts the current sensor readings into the text of the system prompt before every single inference call. That's it. That's the whole integration.

---

*IIIT Hyderabad · RTL AI Tutor v5.1 · Built Sep 2 – Oct 28, 2026 · Submission Nov 10, 2026*
*Note (Sep 2, 2026): AI model loading updated from Qualcomm AI Hub + Genie SDK to llama.cpp + GGUF — Snapdragon 8 Gen 3 (HDK8650) is not supported by any LLM on AI Hub as of this date. All other project tasks unchanged.*
