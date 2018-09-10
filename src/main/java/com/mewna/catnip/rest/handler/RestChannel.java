package com.mewna.catnip.rest.handler;

import com.google.common.collect.ImmutableMap;
import com.mewna.catnip.entity.Embed;
import com.mewna.catnip.entity.Emoji;
import com.mewna.catnip.entity.Message;
import com.mewna.catnip.entity.builder.MessageBuilder;
import com.mewna.catnip.internal.CatnipImpl;
import com.mewna.catnip.rest.ResponsePayload;
import com.mewna.catnip.rest.RestRequester.OutboundRequest;
import com.mewna.catnip.rest.Routes;
import io.vertx.core.json.JsonObject;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author amy
 * @since 9/3/18.
 */
@SuppressWarnings({"unused", "WeakerAccess", "ConstantConditions"})
public class RestChannel extends RestHandler {
    public RestChannel(final CatnipImpl catnip) {
        super(catnip);
    }
    
    // Copied from JDA:
    // https://github.com/DV8FromTheWorld/JDA/blob/9e593c5d5e1abf0967998ac5fcc0d915495e0758/src/main/java/net/dv8tion/jda/core/utils/MiscUtil.java#L179-L198
    // Thank JDA devs! <3
    private static String encodeUTF8(final String chars) {
        try {
            return URLEncoder.encode(chars, "UTF-8");
        } catch(final UnsupportedEncodingException e) {
            throw new AssertionError(e); // thanks JDK 1.4
        }
    }
    
    @Nonnull
    public CompletableFuture<Message> sendMessage(@Nonnull final String channelId, @Nonnull final String content) {
        return sendMessage(channelId, new MessageBuilder().content(content).build());
    }
    
    @Nonnull
    public CompletableFuture<Message> sendMessage(@Nonnull final String channelId, @Nonnull final Embed embed) {
        return sendMessage(channelId, new MessageBuilder().embed(embed).build());
    }
    
    @Nonnull
    public CompletableFuture<Message> sendMessage(@Nonnull final String channelId, @Nonnull final Message message) {
        final JsonObject json = new JsonObject();
        if(message.content() != null && !message.content().isEmpty()) {
            json.put("content", message.content());
        }
        if(message.embeds() != null && !message.embeds().isEmpty()) {
            json.put("embed", getEntityBuilder().embedToJson(message.embeds().get(0)));
        }
        if(json.getValue("embed", null) == null && json.getValue("content", null) == null) {
            throw new IllegalArgumentException("Can't build a message with no content and no embeds!");
        }
        
        return getCatnip().requester().
                queue(new OutboundRequest(Routes.CREATE_MESSAGE.withMajorParam(channelId), ImmutableMap.of(), json))
                .thenApply(ResponsePayload::object)
                .thenApply(getEntityBuilder()::createMessage);
    }
    
    @Nonnull
    @CheckReturnValue
    public CompletableFuture<Message> getMessage(@Nonnull final String channelId, @Nonnull final String messageId) {
        return getCatnip().requester().queue(
                new OutboundRequest(Routes.GET_CHANNEL_MESSAGE.withMajorParam(channelId),
                        ImmutableMap.of("message.id", messageId), null))
                .thenApply(ResponsePayload::object)
                .thenApply(getEntityBuilder()::createMessage);
    }
    
    @Nonnull
    public CompletableFuture<Message> editMessage(@Nonnull final String channelId, @Nonnull final String messageId,
                                                  @Nonnull final String content) {
        return editMessage(channelId, messageId, new MessageBuilder().content(content).build());
    }
    
    @Nonnull
    public CompletableFuture<Message> editMessage(@Nonnull final String channelId, @Nonnull final String messageId,
                                                  @Nonnull final Message message) {
        final JsonObject json = new JsonObject();
        if(message.content() != null && !message.content().isEmpty()) {
            json.put("content", message.content());
        }
        if(message.embeds() != null && !message.embeds().isEmpty()) {
            json.put("embed", getEntityBuilder().embedToJson(message.embeds().get(0)));
        }
        if(json.getValue("embed", null) == null && json.getValue("content", null) == null) {
            throw new IllegalArgumentException("Can't build a message with no content and no embed!");
        }
        return getCatnip().requester()
                .queue(new OutboundRequest(Routes.EDIT_MESSAGE.withMajorParam(channelId),
                        ImmutableMap.of("message.id", messageId), json))
                .thenApply(ResponsePayload::object)
                .thenApply(getEntityBuilder()::createMessage);
    }
    
    @Nonnull
    public CompletableFuture<Void> deleteMessage(@Nonnull final String channelId, @Nonnull final String messageId) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.DELETE_MESSAGE.withMajorParam(channelId),
                ImmutableMap.of("message.id", messageId), null)).thenApply(__ -> null);
    }
    
    @Nonnull
    public CompletableFuture<Void> addReaction(@Nonnull final String channelId, @Nonnull final String messageId,
                                               @Nonnull final String emoji) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.CREATE_REACTION.withMajorParam(channelId),
                ImmutableMap.of("message.id", messageId, "emoji", encodeUTF8(emoji)), new JsonObject()))
                .thenApply(__ -> null);
    }
    
    @Nonnull
    public CompletableFuture<Void> addReaction(@Nonnull final String channelId, @Nonnull final String messageId,
                                               @Nonnull final Emoji emoji) {
        return addReaction(channelId, messageId, emoji.forReaction());
    }
    
    @Nonnull
    public CompletableFuture<Void> deleteOwnReaction(@Nonnull final String channelId, @Nonnull final String messageId,
                                                     @Nonnull final String emoji) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.DELETE_OWN_REACTION.withMajorParam(channelId),
                ImmutableMap.of("message.id", messageId, "emoji", encodeUTF8(emoji)), null))
                .thenApply(__ -> null);
    }
    
    @Nonnull
    public CompletableFuture<Void> deleteOwnReaction(@Nonnull final String channelId, @Nonnull final String messageId,
                                                     @Nonnull final Emoji emoji) {
        return deleteOwnReaction(channelId, messageId, emoji.forReaction());
    }
    
    @Nonnull
    @CheckReturnValue
    public CompletableFuture<List<Message>> getChannelMessages(@Nonnull final String channelId, @Nullable final String before,
                                                               @Nullable final String after, @Nullable final String around,
                                                               @Nonnegative final int limit) {
        final Collection<String> params = new ArrayList<>();
        if(limit > 0) {
            params.add("limit=" + limit);
        }
        if(after != null) {
            params.add("after=" + after);
        }
        if(around != null) {
            params.add("around=" + around);
        }
        if(before != null) {
            params.add("before=" + before);
        }
        String query = String.join("&", params);
        if(!query.isEmpty()) {
            query = '?' + query;
        }
        return getCatnip().requester()
                .queue(new OutboundRequest(Routes.GET_CHANNEL_MESSAGES.withMajorParam(channelId).withQueryString(query),
                        ImmutableMap.of(), null))
                .thenApply(ResponsePayload::array)
                .thenApply(getEntityBuilder()::createManyMessages)
                .thenApply(Collections::unmodifiableList);
    }
    
    @Nonnull
    public CompletableFuture<Void> triggerTypingIndicator(@Nonnull final String channelId) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.TRIGGER_TYPING_INDICATOR.withMajorParam(channelId),
                ImmutableMap.of(), null))
                .thenApply(__ -> null);
    }
}
