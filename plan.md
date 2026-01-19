# AI Chat Application - Implementation Plan

## Overview

A cross-platform AI chat application for **Android and iOS** using Compose Multiplatform. The app connects to a custom/self-hosted OpenAI-compatible API endpoint, renders rich formatted responses (Markdown, code highlighting), and streams responses in real-time.

## Requirements Summary

| Requirement | Decision |
|-------------|----------|
| Platforms | Android + iOS only |
| AI Provider | Custom/self-hosted (OpenAI-compatible API) |
| API Key | User-provided via settings |
| Data Storage | Local only (on device) |
| Streaming | Required |
| UI Style | Material 3 |
| Features | Rich formatting (Markdown, code blocks, syntax highlighting) |

## Architecture

### Module Structure

```
ai-advent-sl/
├── core/
│   ├── coreCommon/          # (existing) Platform abstractions
│   ├── corePref/            # (existing) Preferences - extend for API settings
│   ├── coreNetwork/         # (new) HTTP client, SSE streaming
│   └── coreChat/            # (new) Chat repository, message models, AI service
│
├── ui/
│   ├── uiCommon/            # (existing) Theme, shared resources
│   ├── uiChat/              # (new) Chat screen with message list
│   └── uiSettings/          # (new) Settings screen for API configuration
│
├── navigation/              # (existing) Add new routes
├── composeApp/              # (existing) DI modules, app entry
└── app/
    ├── androidApp/          # (existing) Android entry point
    └── iosApp/              # (existing) iOS entry point
```

### Data Flow

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  UI Layer   │───▶│  ViewModel  │───▶│ Repository  │───▶│  AI Service │
│  (Compose)  │◀───│  (StateFlow)│◀───│  (coreChat) │◀───│ (coreNetwork)│
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
                                             │
                                             ▼
                                    ┌─────────────┐
                                    │  Local DB   │
                                    │ (corePref)  │
                                    └─────────────┘
```

## Technical Decisions

### 1. Networking - Ktor Client
- **Library**: Ktor Client (multiplatform HTTP)
- **Why**: Native multiplatform support, SSE/streaming capability, coroutine-based
- **Engines**: OkHttp (Android), Darwin (iOS)

### 2. Streaming - Server-Sent Events (SSE)
- OpenAI-compatible APIs use SSE for streaming
- Ktor supports SSE via `ktor-client-sse` plugin
- Parse `data: {...}` chunks and emit tokens via `Flow<String>`

### 3. Local Storage - DataStore + Room/SQLDelight
- **Settings**: DataStore (already in corePref) for API endpoint, key, model
- **Chat History**: SQLDelight for message persistence (optional, can start with in-memory)

### 4. Markdown Rendering
- **Library**: `compose-markdown` or custom implementation
- Render: headers, bold, italic, lists, code blocks
- Code highlighting: Use a syntax highlighter library or custom theme

### 5. Dependency Injection - Koin
- Already configured in project
- Add modules for new features

## Implementation Steps

### Phase 1: Core Infrastructure

#### Step 1.1: Create `core:coreNetwork` module
- [ ] Create module with Ktor client setup
- [ ] Configure platform-specific engines (OkHttp/Darwin)
- [ ] Implement SSE streaming support
- [ ] Create `NetworkModule` for Koin

**Files to create:**
```
core/coreNetwork/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/
    │   ├── HttpClientFactory.kt
    │   ├── SseClient.kt
    │   └── NetworkModule.kt
    ├── androidMain/kotlin/
    │   └── HttpEngineFactory.android.kt
    └── iosMain/kotlin/
        └── HttpEngineFactory.ios.kt
```

#### Step 1.2: Create `core:coreChat` module
- [ ] Define data models: `ChatMessage`, `ChatRole`, `Conversation`
- [ ] Create `AiService` interface for API communication
- [ ] Implement `OpenAiCompatibleService` with streaming
- [ ] Create `ChatRepository` for message management
- [ ] Create `ChatModule` for Koin

**Files to create:**
```
core/coreChat/
├── build.gradle.kts
└── src/commonMain/kotlin/
    ├── model/
    │   ├── ChatMessage.kt
    │   ├── ChatRole.kt
    │   └── ApiModels.kt
    ├── service/
    │   ├── AiService.kt
    │   └── OpenAiCompatibleService.kt
    ├── repository/
    │   └── ChatRepository.kt
    └── ChatModule.kt
```

#### Step 1.3: Extend `core:corePref` for settings
- [ ] Add `ApiSettings` data class (endpoint URL, API key, model name)
- [ ] Create preferences accessor methods
- [ ] Secure API key storage

**Files to modify:**
```
core/corePref/src/commonMain/kotlin/
├── model/ApiSettings.kt (new)
└── PreferencesRepository.kt (extend)
```

### Phase 2: UI Implementation

#### Step 2.1: Create `ui:uiSettings` module
- [ ] Settings screen with form fields:
  - API Endpoint URL (text input)
  - API Key (password input)
  - Model name (text input with suggestions)
- [ ] Validation and save functionality
- [ ] SettingsViewModel with state management

**Files to create:**
```
ui/uiSettings/
├── build.gradle.kts
└── src/commonMain/kotlin/
    ├── SettingsScreen.kt
    ├── SettingsViewModel.kt
    ├── SettingsViewState.kt
    └── SettingsModule.kt
```

#### Step 2.2: Create `ui:uiChat` module
- [ ] Chat screen with:
  - Message list (LazyColumn)
  - Input field with send button
  - Loading indicator during streaming
- [ ] Message bubbles with rich formatting
- [ ] Markdown renderer component
- [ ] Code block with syntax highlighting
- [ ] ChatViewModel handling send/receive flow

**Files to create:**
```
ui/uiChat/
├── build.gradle.kts
└── src/commonMain/kotlin/
    ├── ChatScreen.kt
    ├── ChatViewModel.kt
    ├── ChatViewState.kt
    ├── components/
    │   ├── MessageBubble.kt
    │   ├── MessageInput.kt
    │   ├── MarkdownText.kt
    │   └── CodeBlock.kt
    └── ChatModule.kt
```

### Phase 3: Navigation & Integration

#### Step 3.1: Update navigation
- [ ] Add `Chat` route as main destination
- [ ] Add `Settings` route
- [ ] Update splash to navigate to Chat
- [ ] Add settings access from Chat screen

**Files to modify:**
```
navigation/src/commonMain/kotlin/
├── AppRoutes.kt (add Chat, Settings routes)
└── AppNavigation.kt (add navigation graph entries)
```

#### Step 3.2: Wire up DI modules
- [ ] Register all new modules in composeApp
- [ ] Verify dependency graph

**Files to modify:**
```
composeApp/src/commonMain/kotlin/KoinApp.kt
```

### Phase 4: Polish & Platform-Specific

#### Step 4.1: Remove unused platforms
- [ ] Remove `app:desktopApp` module
- [ ] Remove `app:webApp` module
- [ ] Update settings.gradle.kts
- [ ] Clean up platform-specific code in shared modules

#### Step 4.2: Platform optimizations
- [ ] Android: Keyboard handling, back gesture
- [ ] iOS: Safe area insets, keyboard avoidance

#### Step 4.3: Testing
- [ ] Unit tests for ChatRepository
- [ ] Unit tests for API response parsing
- [ ] UI tests for Chat screen

## Dependencies to Add

```toml
# gradle/libs.versions.toml

[versions]
ktor = "3.1.3"
markdown = "0.7.3"

[libraries]
# Ktor Client
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-sse = { module = "io.ktor:ktor-client-sse", version.ref = "ktor" }

# Markdown (evaluate options)
# Option 1: compose-markdown
# Option 2: Custom implementation with basic regex parsing
```

## API Contract (OpenAI-Compatible)

### Request Format
```json
POST /v1/chat/completions
{
  "model": "gpt-4",
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Hello!"}
  ],
  "stream": true
}
```

### Streaming Response Format (SSE)
```
data: {"id":"...","choices":[{"delta":{"content":"Hello"},...}]}
data: {"id":"...","choices":[{"delta":{"content":"!"},...}]}
data: [DONE]
```

## UI Mockup (ASCII)

```
┌─────────────────────────────────┐
│ AI Chat                    [⚙️] │  <- Header with settings
├─────────────────────────────────┤
│                                 │
│  ┌─────────────────────────┐    │
│  │ Hello! How can I help?  │    │  <- AI message (left)
│  └─────────────────────────┘    │
│                                 │
│    ┌─────────────────────────┐  │
│    │ Explain Kotlin coroutines│  │  <- User message (right)
│    └─────────────────────────┘  │
│                                 │
│  ┌─────────────────────────┐    │
│  │ Coroutines are...       │    │
│  │ ```kotlin               │    │  <- Markdown code block
│  │ suspend fun example() { │    │
│  │   delay(1000)           │    │
│  │ }                       │    │
│  │ ```                     │    │
│  └─────────────────────────┘    │
│                                 │
├─────────────────────────────────┤
│ [Message input...        ] [▶️] │  <- Input field
└─────────────────────────────────┘
```

## Success Criteria

1. User can configure API endpoint and key in settings
2. User can send messages and receive streaming responses
3. Markdown formatting renders correctly (headers, bold, code)
4. Code blocks display with proper formatting
5. Chat history persists locally
6. App works on both Android and iOS
7. UI follows Material 3 design guidelines

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| SSE streaming complexity | Start with non-streaming, add streaming as enhancement |
| Markdown rendering edge cases | Use established library or limit supported syntax |
| iOS network permissions | Ensure proper Info.plist configuration |
| API key security | Use platform secure storage (Keychain/EncryptedSharedPrefs) |

## Estimated Effort

| Phase | Scope |
|-------|-------|
| Phase 1 | Core networking and chat infrastructure |
| Phase 2 | UI screens (settings, chat, components) |
| Phase 3 | Navigation and integration |
| Phase 4 | Polish, platform-specific, testing |
