package org.moon.figura.mixin.gui;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.moon.figura.FiguraMod;
import org.moon.figura.avatar.Avatar;
import org.moon.figura.avatar.AvatarManager;
import org.moon.figura.avatar.Badges;
import org.moon.figura.config.Configs;
import org.moon.figura.gui.Emojis;
import org.moon.figura.lua.api.nameplate.NameplateCustomization;
import org.moon.figura.permissions.Permissions;
import org.moon.figura.utils.EntityUtils;
import org.moon.figura.utils.TextUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @ModifyVariable(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", ordinal = 0, argsOnly = true)
    private Component addMessage(Component message, Component msg, MessageSignature signature, int k, GuiMessageTag tag, boolean refresh) {
        //do not change the message on refresh
        if (refresh || AvatarManager.panic)
            return message;

        //receive event
        Avatar localPlayer = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (localPlayer != null) {
            String newMessage = localPlayer.chatReceivedMessageEvent(message);
            //only allow editing if we can parse the messages
            //however allow the event to run, so people can still get those special messages
            if (FiguraMod.parseMessages && newMessage != null)
                message = TextUtils.tryParseJson(newMessage);
        }

        //stop here if we should not parse messages
        if (!FiguraMod.parseMessages)
            return message;

        //emojis
        if (Configs.CHAT_EMOJIS.value)
            message = Emojis.applyEmojis(message);

        //nameplates
        int config = Configs.CHAT_NAMEPLATE.value;
        if (config == 0)
            return message;

        message = TextUtils.parseLegacyFormatting(message);

        Map<String, UUID> players = EntityUtils.getPlayerList();
        String owner = null;

        String[] split = message.getString().split("\\W+");
        for (String s : split) {
            if (players.containsKey(s)) {
                owner = s;
                break;
            }
        }

        //iterate over ALL online players
        for (Map.Entry<String, UUID> entry : players.entrySet()) {
            String name = entry.getKey();
            UUID uuid = entry.getValue();

            Component playerName = Component.literal(name);

            //apply customization
            Avatar avatar = AvatarManager.getAvatarForPlayer(uuid);
            NameplateCustomization custom = avatar == null || avatar.luaRuntime == null ? null : avatar.luaRuntime.nameplate.CHAT;

            Component replacement = custom != null && custom.getJson() != null && avatar.permissions.get(Permissions.NAMEPLATE_EDIT) == 1 ?
                    TextUtils.replaceInText(custom.getJson().copy(), "\n|\\\\n", " ") : playerName;

            //name
            replacement = TextUtils.replaceInText(replacement, "\\$\\{name\\}", playerName);

            //sender badges
            if (config > 1 && name.equals(owner)) {
                //badges
                Component temp = Badges.appendBadges(replacement, uuid, true);
                //trim
                temp = TextUtils.trim(temp);
                //modify message, only first, also no need for ignore case, since it is already matched with proper case
                message = TextUtils.replaceInText(message, "\\b" + Pattern.quote(name) + "\\b", temp, (s, style) -> true, 1);
            }

            //badges
            replacement = Badges.appendBadges(replacement, uuid, config > 1 && owner == null);

            //trim
            replacement = TextUtils.trim(replacement);

            //modify message
            message = TextUtils.replaceInText(message, "(?i)\\b" + Pattern.quote(name) + "\\b", replacement);
        }

        return message;
    }
}
