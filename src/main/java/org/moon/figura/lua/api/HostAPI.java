package org.moon.figura.lua.api;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.brigadier.StringReader;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import org.luaj.vm2.LuaError;
import org.moon.figura.FiguraMod;
import org.moon.figura.avatar.Avatar;
import org.moon.figura.avatar.AvatarManager;
import org.moon.figura.config.Configs;
import org.moon.figura.lua.LuaNotNil;
import org.moon.figura.lua.LuaWhitelist;
import org.moon.figura.lua.api.world.ItemStackAPI;
import org.moon.figura.lua.docs.LuaFieldDoc;
import org.moon.figura.lua.docs.LuaMethodDoc;
import org.moon.figura.lua.docs.LuaMethodOverload;
import org.moon.figura.lua.docs.LuaTypeDoc;
import org.moon.figura.math.vector.FiguraVec3;
import org.moon.figura.mixin.LivingEntityAccessor;
import org.moon.figura.mixin.gui.ChatScreenAccessor;
import org.moon.figura.model.rendering.texture.FiguraTexture;
import org.moon.figura.utils.ColorUtils;
import org.moon.figura.utils.LuaUtils;
import org.moon.figura.utils.TextUtils;

import java.util.*;

@LuaWhitelist
@LuaTypeDoc(
        name = "HostAPI",
        value = "host"
)
public class HostAPI {

    private final Avatar owner;
    private final boolean isHost;
    private final Minecraft minecraft;

    @LuaWhitelist
    @LuaFieldDoc("host.unlock_cursor")
    public boolean unlockCursor = false;
    public Integer chatColor;

    public HostAPI(Avatar owner) {
        this.owner = owner;
        this.minecraft = Minecraft.getInstance();
        this.isHost = owner.isHost;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_host")
    public boolean isHost() {
        return isHost;
    }

    @LuaWhitelist
    @LuaMethodDoc(value = "host.is_cursor_unlocked")
    public boolean isCursorUnlocked() {
        return unlockCursor;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Boolean.class,
                    argumentNames = "boolean"
            ),
            value = "host.set_unlock_cursor")
    public HostAPI setUnlockCursor(boolean bool) {
        unlockCursor = bool;
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "timesData"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, Integer.class},
                            argumentNames = {"fadeInTime", "stayTime", "fadeOutTime"}
                    )
            },
            aliases = "titleTimes",
            value = "host.set_title_times"
    )
    public HostAPI setTitleTimes(Object x, Double y, Double z) {
        if (!isHost()) return this;
        FiguraVec3 times = LuaUtils.parseVec3("setTitleTimes", x, y, z);
        this.minecraft.gui.setTimes((int) times.x, (int) times.y, (int) times.z);
        return this;
    }

    @LuaWhitelist
    public HostAPI titleTimes(Object x, Double y, Double z) {
        return setTitleTimes(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.clear_title")
    public HostAPI clearTitle() {
        if (isHost())
            this.minecraft.gui.clear();
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "title",
            value = "host.set_title"
    )
    public HostAPI setTitle(@LuaNotNil String text) {
        if (isHost())
            this.minecraft.gui.setTitle(TextUtils.tryParseJson(text));
        return this;
    }

    @LuaWhitelist
    public HostAPI title(@LuaNotNil String text) {
        return setTitle(text);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "subtitle",
            value = "host.set_subtitle"
    )
    public HostAPI setSubtitle(@LuaNotNil String text) {
        if (isHost())
            this.minecraft.gui.setSubtitle(TextUtils.tryParseJson(text));
        return this;
    }

    @LuaWhitelist
    public HostAPI subtitle(@LuaNotNil String text) {
        return setSubtitle(text);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "text"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, boolean.class},
                            argumentNames = {"text", "animated"}
                    )
            },
            aliases = "actionbar",
            value = "host.set_actionbar"
    )
    public HostAPI setActionbar(@LuaNotNil String text, boolean animated) {
        if (isHost())
            this.minecraft.gui.setOverlayMessage(TextUtils.tryParseJson(text), animated);
        return this;
    }

    @LuaWhitelist
    public HostAPI actionbar(@LuaNotNil String text, boolean animated) {
        return setActionbar(text, animated);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "message"
            ),
            value = "host.send_chat_message"
    )
    public HostAPI sendChatMessage(@LuaNotNil String message) {
        if (!isHost() || !Configs.CHAT_MESSAGES.value) return this;
        ClientPacketListener connection = this.minecraft.getConnection();
        if (connection != null) connection.sendChat(message);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "command"
            ),
            value = "host.send_chat_command"
    )
    public HostAPI sendChatCommand(@LuaNotNil String command) {
        if (!isHost() || !Configs.CHAT_MESSAGES.value) return this;
        ClientPacketListener connection = this.minecraft.getConnection();
        if (connection != null) connection.sendCommand(command.startsWith("/") ? command.substring(1) : command);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "message"
            ),
            value = "host.append_chat_history"
    )
    public HostAPI appendChatHistory(@LuaNotNil String message) {
        if (isHost())
            this.minecraft.gui.getChat().addRecentChat(message);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Boolean.class,
                            argumentNames = "offhand"
                    )
            },
            value = "host.swing_arm"
    )
    public HostAPI swingArm(boolean offhand) {
        if (isHost() && this.minecraft.player != null)
            this.minecraft.player.swing(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "slot"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = Integer.class,
                            argumentNames = "slot"
                    )
            },
            value = "host.get_slot"
    )
    public ItemStackAPI getSlot(@LuaNotNil Object slot) {
        if (!isHost()) return null;

        Integer index;
        if (slot instanceof String s) {
            try {
                index = SlotArgument.slot().parse(new StringReader(s));
            } catch (Exception e) {
                throw new LuaError("Unable to get slot \"" + slot + "\"");
            }
        } else if (slot instanceof Integer i)
            index = i;
        else {
            throw new LuaError("Invalid type for getSlot: " + slot.getClass().getSimpleName());
        }

        Entity e = this.owner.luaRuntime.getUser();
        return ItemStackAPI.verify(e.getSlot(index).get());
    }

    @LuaWhitelist
    public HostAPI setBadge(int index, boolean value, boolean pride) {
        if (!isHost()) return this;
        if (!FiguraMod.DEBUG_MODE)
            throw new LuaError("Congrats, you found this debug easter egg!");

        Pair<BitSet, BitSet> badges = AvatarManager.getBadges(owner.owner);
        if (badges == null)
            return this;

        BitSet set = pride ? badges.getFirst() : badges.getSecond();
        set.set(index, value);
        return this;
    }

    @LuaWhitelist
    public HostAPI badge(int index, boolean value, boolean pride) {
        return setBadge(index, value, pride);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_chat_color")
    public Integer getChatColor() {
        return isHost() ? this.chatColor : null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "color"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"r", "g", "b"}
                    )
            },
            aliases = "chatColor",
            value = "host.set_chat_color"
    )
    public HostAPI setChatColor(Object x, Double y, Double z) {
        if (isHost()) this.chatColor = x == null ? null : ColorUtils.rgbToInt(LuaUtils.parseVec3("setChatColor", x, y, z));
        return this;
    }

    @LuaWhitelist
    public HostAPI chatColor(Object x, Double y, Double z) {
        return setChatColor(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_chat_text")
    public String getChatText() {
        if (isHost() && this.minecraft.screen instanceof ChatScreen chat)
            return ((ChatScreenAccessor) chat).getInput().getValue();

        return null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "chatText",
            value = "host.set_chat_text"
    )
    public HostAPI setChatText(@LuaNotNil String text) {
        if (isHost() && Configs.CHAT_MESSAGES.value && this.minecraft.screen instanceof ChatScreen chat)
            ((ChatScreenAccessor) chat).getInput().setValue(text);
        return this;
    }

    @LuaWhitelist
    public HostAPI chatText(@LuaNotNil String text) {
        return setChatText(text);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_screen")
    public String getScreen() {
        if (!isHost() || this.minecraft.screen == null)
            return null;
        return this.minecraft.screen.getClass().getName();
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_chat_open")
    public boolean isChatOpen() {
        return isHost() && this.minecraft.screen instanceof ChatScreen;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_container_open")
    public boolean isContainerOpen() {
        return isHost() && this.minecraft.screen instanceof AbstractContainerScreen;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "name"
            ),
            value = "host.screenshot")
    public FiguraTexture screenshot(@LuaNotNil String name) {
        if (!isHost())
            return null;

        NativeImage img = Screenshot.takeScreenshot(this.minecraft.getMainRenderTarget());
        return owner.luaRuntime.texture.register(name, img, true);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_avatar_uploaded")
    public boolean isAvatarUploaded() {
        return isHost() && AvatarManager.localUploaded;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_status_effects")
    public List<Map<String, Object>> getStatusEffects() {
        List<Map<String, Object>> list = new ArrayList<>();

        LocalPlayer player = this.minecraft.player;
        if (!isHost() || player == null)
            return list;

        for (MobEffectInstance effect : player.getActiveEffects()) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", effect.getEffect().getDescriptionId());
            map.put("amplifier", effect.getAmplifier());
            map.put("duration", effect.getDuration());
            map.put("visible", effect.isVisible());

            list.add(map);
        }

        return list;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_clipboard")
    public String getClipboard() {
        return isHost() ? this.minecraft.keyboardHandler.getClipboard() : null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "clipboard",
            value = "host.set_clipboard")
    public HostAPI setClipboard(@LuaNotNil String text) {
        if (isHost()) this.minecraft.keyboardHandler.setClipboard(text);
        return this;
    }

    @LuaWhitelist
    public HostAPI clipboard(@LuaNotNil String text) {
        return setClipboard(text);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_attack_charge")
    public float getAttackCharge() {
        LocalPlayer player = this.minecraft.player;
        if (isHost() && player != null)
            return player.getAttackStrengthScale(0f);
        return 0f;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_jumping")
    public boolean isJumping() {
        LocalPlayer player = this.minecraft.player;
        if (isHost() && player != null)
            return ((LivingEntityAccessor) player).isJumping();
        return false;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_flying")
    public boolean isFlying() {
        LocalPlayer player = this.minecraft.player;
        if (isHost() && player != null)
            return player.getAbilities().flying;
        return false;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_reach_distance")
    public double getReachDistance() {
        return this.minecraft.gameMode == null ? 0 : this.minecraft.gameMode.getPickRange();
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_air")
    public int getAir() {
        LocalPlayer player = this.minecraft.player;
        if (isHost() && player != null)
            return player.getAirSupply();
        return 0;
    }

    public Object __index(String arg) {
        if ("unlockCursor".equals(arg))
            return unlockCursor;
        return null;
    }

    @LuaWhitelist
    public void __newindex(@LuaNotNil String key, Object value) {
        if ("unlockCursor".equals(key))
            unlockCursor = (Boolean) value;
        else throw new LuaError("Cannot assign value on key \"" + key + "\"");
    }

    @Override
    public String toString() {
        return "HostAPI";
    }
}
