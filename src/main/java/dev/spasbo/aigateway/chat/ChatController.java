package dev.spasbo.aigateway.chat;

import dev.spasbo.aigateway.identity.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Temporary milestone-1 endpoint (F1); replaced by the conversation endpoints in milestone 4. */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    /** A cheap guard ahead of the model's context window; real token-based trimming arrives with F5. */
    static final int MAX_MESSAGE_CHARS = 16_000;

    private final ModelAllowlist allowlist;
    private final ChatService chatService;

    public ChatController(ModelAllowlist allowlist, ChatService chatService) {
        this.allowlist = allowlist;
        this.chatService = chatService;
    }

    @PostMapping
    ChatReply chat(@Valid @RequestBody ChatRequest request, CurrentUser caller) {
        ModelDefinition model = allowlist.find(request.model())
                .orElseThrow(() -> new UnknownModelException(request.model()));
        return ChatReply.from(chatService.chat(caller, model, request.message()));
    }
}
