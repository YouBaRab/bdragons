package com.yb.dragons.client.screen;

import com.yb.dragons.Config;
import com.yb.dragons.ModRegistry;
import com.yb.dragons.client.ClientDragonData;
import com.yb.dragons.client.ClientLairData;
import com.yb.dragons.client.ClientSetup;
import com.yb.dragons.client.hud.DragonHud;
import com.yb.dragons.dragon.DragonAlpha;
import com.yb.dragons.dragon.DragonForm;
import com.yb.dragons.dragon.DragonQuests;
import com.yb.dragons.dragon.DragonRecipes;
import com.yb.dragons.dragon.DragonStage;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.network.ModNetwork;
import com.yb.dragons.player.DragonPlayerData;
import com.yb.dragons.player.DragonPlayerManager;
import com.yb.dragons.world.lair.LairBiomes;
import com.yb.dragons.world.lair.LairManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Dragon Codex: a two-page book with everything there is to discover in the mod
 * (progression, quests, essences, the 11 races and their look at each stage, the 2 forms, the 5 stages, the alpha,
 * powers and keys, wild dragons and their lairs, items and recipes, tips).
 *
 * <p>The text comes from the language files and the numbers come from the code itself, so the
 * book is always right. Pages are laid out at opening time with the real font, so nothing is cut.</p>
 */
public class CodexScreen extends Screen {
    private static final int PAGE_W = 150;
    private static final int BOOK_W = PAGE_W * 2;
    private static final int BOOK_H = 196;
    private static final int MARGIN = 12;
    private static final int TEXT_W = PAGE_W - 2 * MARGIN;
    private static final int CONTENT_TOP = 12;
    private static final int CONTENT_H = BOOK_H - CONTENT_TOP - 22;

    private static final int INK = 0xFF3B2A1A;
    private static final int MUTED = 0xFF6E5A44;
    private static final int TITLE = 0xFF7A1F1F;
    private static final int GOOD = 0xFF2E6B1F;
    private static final int BAD = 0xFF9A2A1A;
    private static final int GALLERY_H = 162;
    /** Cells of the race picture atlas (360 x 360): x, y, w, h in texture pixels (3 per screen pixel). */
    private static final int[][] GALLERY_CELLS = {{0, 0, 180, 114}, {180, 0, 180, 114}, {0, 114, 180, 114}, {180, 114, 180, 114}, {75, 228, 210, 132}};

    private static int lastSpread;

    private final List<List<Block>> pages = new ArrayList<>();
    private final List<Chapter> chapters = new ArrayList<>();
    private final List<Hover> hovers = new ArrayList<>();
    private final List<LinkArea> links = new ArrayList<>();
    private int spread;
    private int left;
    private int top;
    private PageButton backButton;
    private PageButton forwardButton;

    // --- layout blocks ---------------------------------------------------------------------------------------
    private sealed interface Block permits Line, Heading, ItemRow, Recipe, Gap, Bar, Link, Gallery, Picture {}

    private record Line(FormattedCharSequence text, int color) implements Block {}

    /** The last line of a title, underlined (the first lines of a long title are plain {@link Line}s). */
    private record Heading(FormattedCharSequence text, int color, boolean big) implements Block {}

    /** A row of item icons (with counts), and an optional text after them. */
    private record ItemRow(List<ItemStack> items, @Nullable Component label, int color) implements Block {}

    /** A 3x3 crafting grid (shaped) or a list of ingredients (shapeless) and the result. */
    private record Recipe(List<ItemStack> grid, ItemStack result, boolean shapeless) implements Block {}

    private record Gap(int height) implements Block {}

    private record Bar(float fraction, int color) implements Block {}

    /** The look of a race at the 5 stages (pictures of the real model), a full page. */
    private record Gallery(DragonType type) implements Block {}

    /** A picture of the real model (the whole width of the page). */
    private record Picture(Identifier texture, int height) implements Block {}

    /** A clickable line of the table of contents. */
    private record Link(Component text, int chapter) implements Block {}

    private record Chapter(Component title, int firstPage) {}

    private record Hover(int x, int y, int w, int h, ItemStack stack) {}

    private record LinkArea(int x, int y, int w, int h, int page) {}

    private static int height(Block b) {
        return switch (b) {
            case Line l -> 10;
            case Heading h -> h.big() ? 16 : 13;
            case ItemRow i -> 19;
            case Recipe r -> r.shapeless() ? 22 : 58;
            case Gap g -> g.height();
            case Bar b2 -> 8;
            case Link l -> 11;
            case Gallery g -> GALLERY_H;
            case Picture p -> p.height() + 3;
        };
    }

    public CodexScreen() {
        super(Component.translatable("screen.bdragon.codex"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------------------------------------------
    // Init and navigation
    // ------------------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------------------
    // Language of the codex: the language of the game, or English / French chosen in the codex
    // ------------------------------------------------------------------------------------------------------

    /** The language chosen in the codex ("" = the language of the game), kept in config/bdragon-codex-lang.txt. */
    private static @Nullable String codexLang;
    private static @Nullable Language loaded;
    private static String loadedCode = "";

    private static Path langFile() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("bdragon-codex-lang.txt");
    }

    private static String codexLang() {
        if (codexLang == null) {
            try {
                Path f = langFile();
                codexLang = Files.exists(f) ? Files.readString(f).trim() : "";
            } catch (Exception e) {
                codexLang = "";
            }
        }
        return codexLang;
    }

    /** The code of the language the codex is shown in. */
    private static String shownLang() {
        String c = codexLang();
        if (!c.isEmpty()) return c;
        return Minecraft.getInstance().getLanguageManager().getSelected().startsWith("fr") ? "fr_fr" : "en_us";
    }

    /** The language to show the codex in, or null for the language of the game. */
    private static @Nullable Language forced() {
        String c = codexLang();
        if (c.isEmpty()) return null;
        if (loaded == null || !loadedCode.equals(c)) {
            loaded = ClientLanguage.loadFrom(Minecraft.getInstance().getResourceManager(), List.of("en_us", c), false);
            loadedCode = c;
        }
        return loaded;
    }

    /** Runs the action with the language of the codex. */
    private static void inCodexLang(Runnable action) {
        Language lang = forced();
        if (lang == null) {
            action.run();
            return;
        }
        Language previous = Language.getInstance();
        Language.inject(lang);
        try {
            action.run();
        } finally {
            Language.inject(previous);
        }
    }

    /** The EN / FR button: switches the language of the codex. */
    private void switchLang() {
        codexLang = shownLang().equals("fr_fr") ? "en_us" : "fr_fr";
        try {
            Files.createDirectories(langFile().getParent());
            Files.writeString(langFile(), codexLang);
        } catch (Exception ignored) {
            // only a convenience: the choice is kept for this session
        }
        lastSpread = spread;
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        inCodexLang(this::initInLang);
    }

    private void initInLang() {
        left = (this.width - BOOK_W) / 2;
        top = Math.max(2, (this.height - BOOK_H - 26) / 2);
        buildPages();
        spread = Math.min(lastSpread, maxSpread());

        backButton = this.addRenderableWidget(new PageButton(left + 10, top + BOOK_H - 20, false, b -> turn(-1), true));
        forwardButton = this.addRenderableWidget(new PageButton(left + BOOK_W - 33, top + BOOK_H - 20, true, b -> turn(1), true));
        this.addRenderableWidget(Button.builder(Component.translatable("screen.bdragon.codex.contents"), b -> goToPage(0))
                .bounds(this.width / 2 - 102, top + BOOK_H + 4, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(this.width / 2 + 2, top + BOOK_H + 4, 100, 20).build());
        // EN / FR: the language the codex switches to
        this.addRenderableWidget(Button.builder(Component.literal(shownLang().equals("fr_fr") ? "EN" : "FR"), b -> switchLang())
                .bounds(this.width / 2 + 106, top + BOOK_H + 4, 26, 20).build());
        updateButtons();
    }

    private int maxSpread() {
        return Math.max(0, (pages.size() - 1) / 2);
    }

    private void turn(int delta) {
        spread = Math.max(0, Math.min(maxSpread(), spread + delta));
        lastSpread = spread;
        updateButtons();
    }

    private void goToPage(int page) {
        spread = Math.max(0, Math.min(maxSpread(), page / 2));
        lastSpread = spread;
        updateButtons();
    }

    private void updateButtons() {
        if (backButton != null) backButton.visible = spread > 0;
        if (forwardButton != null) forwardButton.visible = spread < maxSpread();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) return true;
        if (event.isLeft() || event.shortcutKey() == 1073741899) {
            turn(-1);
            return true;
        }
        if (event.isRight() || event.shortcutKey() == 1073741902) {
            turn(1);
            return true;
        }
        if (ClientSetup.KEY_GRIMOIRE.matches(event)) {
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) {
            turn(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (LinkArea area : links) {
                if (event.x() >= area.x() && event.x() < area.x() + area.w() && event.y() >= area.y() && event.y() < area.y() + area.h()) {
                    goToPage(area.page());
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    // ------------------------------------------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int right = left + BOOK_W, bottom = top + BOOK_H;
        // leather cover
        g.fill(left - 7, top - 6, right + 7, bottom + 6, 0xFF2A160A);
        g.fill(left - 6, top - 5, right + 6, bottom + 5, 0xFF5A2E16);
        // the two pages
        g.fill(left, top, left + PAGE_W, bottom, 0xFFF1E3C2);
        g.fill(left + PAGE_W, top, right, bottom, 0xFFF4E7C8);
        // shadow of the binding
        g.fill(left + PAGE_W - 2, top, left + PAGE_W + 2, bottom, 0xFFD6C29A);
        g.fill(left + PAGE_W - 1, top, left + PAGE_W + 1, bottom, 0xFFBFA77A);
        // thin gold lines
        g.fill(left + 4, top + 4, left + PAGE_W - 6, top + 5, 0xFFC9A44A);
        g.fill(left + PAGE_W + 6, top + 4, right - 4, top + 5, 0xFFC9A44A);
        g.fill(left + 4, bottom - 5, left + PAGE_W - 6, bottom - 4, 0xFFC9A44A);
        g.fill(left + PAGE_W + 6, bottom - 5, right - 4, bottom - 4, 0xFFC9A44A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        inCodexLang(() -> extractInLang(g, mouseX, mouseY, a));
    }

    private void extractInLang(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        hovers.clear();
        links.clear();
        int leftPage = spread * 2;
        drawPage(g, leftPage, left + MARGIN);
        drawPage(g, leftPage + 1, left + PAGE_W + MARGIN);

        for (Hover h : hovers) {
            if (mouseX >= h.x() && mouseX < h.x() + h.w() && mouseY >= h.y() && mouseY < h.y() + h.h()) {
                g.setTooltipForNextFrame(this.font, h.stack(), mouseX, mouseY);
                break;
            }
        }
    }

    private void drawPage(GuiGraphicsExtractor g, int index, int x) {
        if (index < 0 || index >= pages.size()) return;
        int y = top + CONTENT_TOP;
        for (Block block : pages.get(index)) {
            switch (block) {
                case Line l -> g.text(this.font, l.text(), x, y, l.color(), false);
                case Heading h -> {
                    g.text(this.font, h.text(), x, y + (h.big() ? 2 : 1), h.color(), false);
                    g.fill(x, y + (h.big() ? 12 : 10), x + TEXT_W, y + (h.big() ? 13 : 11), (h.color() & 0x00FFFFFF) | 0x80000000);
                }
                case ItemRow it -> {
                    int ix = x;
                    for (ItemStack stack : it.items()) {
                        drawItem(g, stack, ix, y + 1);
                        ix += 18;
                    }
                    if (it.label() != null) {
                        int avail = x + TEXT_W - ix - 2;
                        List<FormattedCharSequence> lines = this.font.split(it.label(), Math.max(20, avail));
                        if (lines.size() == 1) g.text(this.font, lines.get(0), ix + 2, y + 5, it.color(), false);
                        else {
                            g.text(this.font, lines.get(0), ix + 2, y + 1, it.color(), false);
                            g.text(this.font, lines.get(1), ix + 2, y + 10, it.color(), false);
                        }
                    }
                }
                case Recipe r -> drawRecipe(g, r, x, y);
                case Gap gap -> {
                }
                case Bar bar -> {
                    float f = Math.max(0, Math.min(1, bar.fraction()));
                    g.fill(x, y + 2, x + TEXT_W, y + 6, 0xFF8C7A5E);
                    g.fill(x + 1, y + 3, x + TEXT_W - 1, y + 5, 0xFFE8D9B5);
                    g.fill(x + 1, y + 3, x + 1 + Math.round((TEXT_W - 2) * f), y + 5, bar.color());
                }
                case Gallery gal -> drawGallery(g, gal.type(), x, y);
                case Picture pic -> g.blit(pic.texture(), x, y + 1, x + TEXT_W, y + 1 + pic.height(), 0, 1, 0, 1);
                case Link link -> {
                    Chapter c = chapters.get(link.chapter());
                    String num = String.valueOf(c.firstPage() + 1);
                    g.text(this.font, link.text(), x + 4, y + 1, TITLE, false);
                    g.text(this.font, num, x + TEXT_W - this.font.width(num), y + 1, MUTED, false);
                    links.add(new LinkArea(x, y, TEXT_W, 11, c.firstPage()));
                }
            }
            y += height(block);
        }
        String number = String.valueOf(index + 1);
        g.text(this.font, number, x + TEXT_W / 2 - this.font.width(number) / 2, top + BOOK_H - 15, MUTED, false);
    }

    /** Title, then the stages I-IV in two rows and the legendary stage, bigger, below. */
    private void drawGallery(GuiGraphicsExtractor g, DragonType type, int x, int y) {
        Component title = Component.translatable("codex.bdragon.races.gallery").withStyle(ChatFormatting.BOLD);
        g.text(this.font, title, x + TEXT_W / 2 - this.font.width(title) / 2, y + 1, 0xFF000000 | type.inkColor(), false);
        var texture = com.yb.dragons.YBDragons.id("textures/gui/codex/" + type.id() + ".png");
        int[][] places = {{0, 12}, {66, 12}, {0, 60}, {66, 60}, {28, 108}};
        for (int i = 0; i < 5; i++) {
            int[] c = GALLERY_CELLS[i];
            int w = c[2] / 3, h = c[3] / 3;
            int px = x + places[i][0], py = y + places[i][1];
            g.blit(texture, px, py, px + w, py + h, c[0] / 360F, (c[0] + c[2]) / 360F, c[1] / 360F, (c[1] + c[3]) / 360F);
            DragonStage stage = DragonStage.byNumber(i + 1);
            Component label = Component.literal(DragonHud.roman(i + 1) + " · ").append(stage.displayName());
            if (this.font.width(label) > w + 4) label = Component.literal(DragonHud.roman(i + 1));
            g.text(this.font, label, px + w / 2 - this.font.width(label) / 2, py + h + 1, MUTED, false);
        }
    }

    private void drawItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        g.item(stack, x, y);
        g.itemDecorations(this.font, stack, x, y);
        hovers.add(new Hover(x, y, 16, 16, stack));
    }

    private void drawRecipe(GuiGraphicsExtractor g, Recipe r, int x, int y) {
        if (r.shapeless()) {
            int ix = x + 1;
            for (ItemStack s : r.grid()) {
                slot(g, ix - 1, y + 1);
                drawItem(g, s, ix, y + 2);
                ix += 19;
            }
            g.text(this.font, "→", ix + 2, y + 6, INK, false);
            slot(g, ix + 13, y + 1);
            drawItem(g, r.result(), ix + 14, y + 2);
            return;
        }
        for (int i = 0; i < 9; i++) {
            int sx = x + 1 + (i % 3) * 18, sy = y + 2 + (i / 3) * 18;
            slot(g, sx - 1, sy - 1);
            drawItem(g, r.grid().get(i), sx, sy);
        }
        g.text(this.font, "→", x + 62, y + 22, INK, false);
        slot(g, x + 77, y + 19);
        drawItem(g, r.result(), x + 78, y + 20);
    }

    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF8C7A5E);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFFD9C8A3);
    }

    // ------------------------------------------------------------------------------------------------------
    // Pagination
    // ------------------------------------------------------------------------------------------------------

    private List<Block> current;
    private int used;

    private void newPage() {
        current = new ArrayList<>();
        pages.add(current);
        used = 0;
    }

    private void add(Block b) {
        int h = height(b);
        if (current == null || used + h > CONTENT_H) {
            newPage();
            if (b instanceof Gap) return;
        }
        current.add(b);
        used += h;
    }

    /** A new chapter always starts on a new page. */
    private void chapter(String key) {
        if (current == null || !current.isEmpty()) newPage();
        chapters.add(new Chapter(Component.translatable(key), pages.size() - 1));
        bigHeading(Component.translatable(key), TITLE);
    }

    /** Big bold title (chapters, races), wrapped if it is too long for the page. */
    private void bigHeading(Component text, int color) {
        List<FormattedCharSequence> lines = this.font.split(text.copy().withStyle(ChatFormatting.BOLD), TEXT_W);
        for (int i = 0; i < lines.size() - 1; i++) add(new Line(lines.get(i), color));
        add(new Heading(lines.get(lines.size() - 1), color, true));
    }

    private void heading(Component text, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, TEXT_W);
        int h = (lines.size() - 1) * 10 + 13;
        // keep a heading with at least two lines of its section
        if (current != null && used + h + 20 > CONTENT_H) newPage();
        if (current != null && used > 0) add(new Gap(4));
        for (int i = 0; i < lines.size() - 1; i++) add(new Line(lines.get(i), color));
        add(new Heading(lines.get(lines.size() - 1), color, false));
    }

    private void heading(String key) {
        heading(Component.translatable(key), TITLE);
    }

    private void text(Component text, int color) {
        for (FormattedCharSequence line : this.font.split(text, TEXT_W)) add(new Line(line, color));
    }

    private void text(String key, Object... args) {
        text(Component.translatable(key, args), INK);
    }

    private void muted(String key, Object... args) {
        text(Component.translatable(key, args).withStyle(ChatFormatting.ITALIC), MUTED);
    }

    private void gap() {
        if (current != null && used > 0) add(new Gap(4));
    }

    private void items(List<ItemStack> stacks, @Nullable Component label, int color) {
        add(new ItemRow(stacks, label, color));
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static List<ItemStack> grid(Item[] items) {
        List<ItemStack> list = new ArrayList<>();
        for (Item i : items) list.add(i == null ? ItemStack.EMPTY : new ItemStack(i));
        return list;
    }

    private static Component key(net.minecraft.client.KeyMapping mapping) {
        return Component.literal("[").append(mapping.getTranslatedKeyMessage()).append("]").withStyle(ChatFormatting.BOLD);
    }

    // ------------------------------------------------------------------------------------------------------
    // Content
    // ------------------------------------------------------------------------------------------------------

    private void buildPages() {
        pages.clear();
        chapters.clear();
        current = null;
        // page 1 is the table of contents, filled at the end when the page numbers are known
        newPage();
        List<Block> contents = current;
        current = null;

        progressChapter();
        becomeChapter();
        essenceChapter();
        racesChapter();
        formsChapter();
        stagesChapter();
        alphaChapter();
        powersChapter();
        lairsChapter();
        itemsChapter();
        tipsChapter();

        // the table of contents must fit on one page: title, a short hint (2 lines max) and one line per chapter
        contents.add(new Heading(Component.translatable("screen.bdragon.codex").withStyle(ChatFormatting.BOLD).getVisualOrderText(), TITLE, true));
        List<FormattedCharSequence> hint = this.font.split(Component.translatable("codex.bdragon.contents.hint").withStyle(ChatFormatting.ITALIC), TEXT_W);
        for (int i = 0; i < Math.min(2, hint.size()); i++) contents.add(new Line(hint.get(i), MUTED));
        contents.add(new Gap(4));
        for (int i = 0; i < chapters.size(); i++) contents.add(new Link(chapters.get(i).title(), i));
    }

    private void progressChapter() {
        DragonPlayerData data = ClientDragonData.own();
        chapter("codex.bdragon.progress");
        DragonType type = data.type();
        if (type == null) {
            if (data.questsDone() && data.chosenType() != null) {
                heading("codex.bdragon.progress.ritual");
                text("codex.bdragon.progress.ritual.text", data.chosenType().displayName());
                return;
            }
            text("codex.bdragon.progress.human");
            int quest = data.questIndex();
            heading(Component.translatable("codex.bdragon.progress.quest", quest + 1, DragonQuests.QUEST_COUNT, DragonQuests.title(quest)), TITLE);
            muted("quest.bdragon." + (quest + 1) + ".lore");
            gap();
            if (quest == DragonQuests.ESSENCE_QUEST) {
                text("codex.bdragon.progress.essence");
                List<ItemStack> row = new ArrayList<>();
                for (DragonType t : DragonType.SELECTABLE) {
                    row.add(new ItemStack(ModRegistry.ESSENCES.get(t).get()));
                    if (row.size() == 6) {
                        items(row, null, INK);
                        row = new ArrayList<>();
                    }
                }
                if (!row.isEmpty()) items(row, null, INK);
                return;
            }
            List<DragonQuests.Requirement> reqs = DragonQuests.requirements(quest, data.chosenType());
            for (int i = 0; i < reqs.size(); i++) {
                DragonQuests.Requirement r = reqs.get(i);
                int have = Math.min(data.questProgress()[i], r.count());
                boolean done = have >= r.count();
                items(List.of(new ItemStack(r.icon())), Component.literal(have + " / " + r.count() + "  ").append(r.name()), done ? GOOD : INK);
            }
            gap();
            muted("codex.bdragon.progress.altar");
            return;
        }

        DragonStage stage = data.stage();
        heading(type.displayName().copy().withStyle(ChatFormatting.BOLD), 0xFF000000 | type.inkColor());
        text("codex.bdragon.progress.stage", stage.number(), data.isAlpha() ? Component.translatable("dragon_stage.bdragon.alpha") : stage.displayName());
        text("codex.bdragon.progress.form", data.form().displayName());
        gap();
        if (data.isAlpha()) {
            text(Component.translatable("codex.bdragon.progress.alpha"), GOOD);
        } else if (stage.isMax()) {
            text(Component.translatable("codex.bdragon.progress.max"), GOOD);
            gap();
            text("codex.bdragon.progress.alpha_next");
            items(List.of(new ItemStack(ModRegistry.ALPHA_SCALES.get(type).get(), Math.max(1, data.forgeScales(type))), new ItemStack(Items.NETHER_STAR)),
                    Component.translatable("codex.bdragon.progress.forge", data.forgeScales(type), DragonAlpha.SCALES_PER_ORB, data.forgeStars()), INK);
        } else {
            DragonStage next = stage.next();
            text("codex.bdragon.progress.power", data.power(), next.powerRequired());
            float from = stage.powerRequired();
            add(new Bar((data.power() - from) / (next.powerRequired() - from), 0xFF8A4FD8));
            boolean ok = data.maxDefeatedRank() >= next.challengeRank();
            text(Component.translatable("codex.bdragon.progress.challenge", DragonHud.roman(next.challengeRank()))
                    .append(ok ? " ✔" : " ✘"), ok ? GOOD : BAD);
            gap();
            text("codex.bdragon.progress.next", next.number(), next.featureKey());
        }
        gap();
        text("codex.bdragon.progress.defeated", data.dragonsDefeated());
        text("codex.bdragon.progress.lairs", ClientLairData.current().stream().filter(ModNetwork.LairSync.Entry::known).count());
    }

    private void becomeChapter() {
        chapter("codex.bdragon.become");
        text("codex.bdragon.become.intro");
        heading("block.bdragon.dragon_altar");
        add(new Recipe(grid(DragonRecipes.ALTAR), new ItemStack(ModRegistry.DRAGON_ALTAR_ITEM.get()), false));
        text("codex.bdragon.become.altar");
        for (int q = 0; q < DragonQuests.QUEST_COUNT; q++) {
            heading(Component.translatable("codex.bdragon.become.quest", q + 1, DragonQuests.title(q)), TITLE);
            muted("quest.bdragon." + (q + 1) + ".lore");
            if (q < DragonQuests.ESSENCE_QUEST) {
                List<ItemStack> row = new ArrayList<>();
                for (DragonQuests.Requirement r : DragonQuests.requirements(q, null)) row.add(stack(r.icon(), r.count()));
                items(row, null, INK);
            } else if (q == DragonQuests.ESSENCE_QUEST) {
                text("codex.bdragon.become.quest3");
            } else {
                text("codex.bdragon.become.quest4");
            }
        }
        heading("codex.bdragon.become.ritual");
        text("codex.bdragon.become.ritual.text");
    }

    private void essenceChapter() {
        chapter("codex.bdragon.essences");
        text("codex.bdragon.essences.intro");
        for (DragonType type : DragonType.SELECTABLE) {
            gap();
            text(type.displayName().copy().withStyle(ChatFormatting.BOLD), 0xFF000000 | type.inkColor());
            List<ItemStack> ings = new ArrayList<>();
            for (Item i : DragonRecipes.ESSENCES.get(type)) ings.add(new ItemStack(i));
            ings.add(new ItemStack(Items.GLASS_BOTTLE));
            add(new Recipe(ings, new ItemStack(ModRegistry.ESSENCES.get(type).get()), true));
        }
    }

    private void racesChapter() {
        chapter("codex.bdragon.races");
        text("codex.bdragon.races.intro");
        for (DragonType type : DragonType.SELECTABLE) {
            // a race never starts at the very bottom of a page
            if (used > 0 && used + 70 > CONTENT_H) newPage();
            else gap();
            int color = 0xFF000000 | type.inkColor();
            bigHeading(type.displayName(), color);
            muted("dragon_type.bdragon." + type.id() + ".desc");
            gap();
            text(label("codex.bdragon.races.powers"), INK);
            text(Component.translatable("dragon_type.bdragon." + type.id() + ".powers"), INK);
            gap();
            text(label("codex.bdragon.races.offerings"), INK);
            List<ItemStack> row = new ArrayList<>();
            for (DragonQuests.Requirement r : DragonQuests.requirements(DragonQuests.OFFERING_QUEST, type)) row.add(stack(r.icon(), r.count()));
            items(row, null, INK);
        }
    }

    private static MutableComponent label(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.BOLD).append(Component.literal(" ").withStyle(ChatFormatting.RESET));
    }

    private static Component biomes(DragonType type) {
        MutableComponent c = Component.empty();
        List<ResourceKey<Biome>> list = LairBiomes.biomesOf(type);
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) c.append(", ");
            var id = list.get(i).identifier();
            c.append(Component.translatable("biome." + id.getNamespace() + "." + id.getPath()));
        }
        return c;
    }

    private void formsChapter() {
        chapter("codex.bdragon.forms");
        text("codex.bdragon.forms.intro", key(ClientSetup.KEY_FORM));
        heading(DragonForm.HYBRID.displayName(), TITLE);
        text("codex.bdragon.forms.hybrid", pct(DragonForm.HYBRID.powerFactor()));
        heading(DragonForm.DRAGON.displayName(), TITLE);
        text("codex.bdragon.forms.dragon");
    }

    private static String pct(float f) {
        return Math.round(f * 100) + "%";
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((int) d) : String.format(java.util.Locale.ROOT, "%.1f", d);
    }

    private void stagesChapter() {
        chapter("codex.bdragon.stages");
        text("codex.bdragon.stages.intro");
        gap();
        text("codex.bdragon.stages.power");
        for (DragonStage s : DragonStage.values()) {
            heading(Component.translatable("codex.bdragon.stages.title", DragonHud.roman(s.number()), s.displayName()), TITLE);
            Component challenge = s.challengeRank() == 0 ? Component.literal("—") : Component.literal(DragonHud.roman(s.challengeRank()));
            text("codex.bdragon.stages.need", s.powerRequired(), challenge);
            text(label("codex.bdragon.stages.look").append(s.featureKey()), INK);
            text("codex.bdragon.stages.stats", s.bonusHealth() / 2, s.bonusArmor(), s.bonusAttack());
            text("codex.bdragon.stages.breath", s.breathRange(), String.format(java.util.Locale.ROOT, "%.1f", s.breathDamage()), s.maxEnergy());
            String fly = !s.canFly() ? "codex.bdragon.stages.fly.no" : s.limitedFlight() ? "codex.bdragon.stages.fly.limited" : "codex.bdragon.stages.fly.yes";
            text(label("codex.bdragon.stages.fly").append(Component.translatable(fly)), INK);
            text("codex.bdragon.stages.sense", s.senseRadius());
        }
        gap();
        text(Component.translatable("codex.bdragon.stages.alpha"), TITLE);
    }

    /** The alpha: the alpha scales, the forge of the orb at the altar, the awakening and what the alpha gets. */
    private void alphaChapter() {
        chapter("codex.bdragon.alpha");
        text("codex.bdragon.alpha.intro");
        heading("codex.bdragon.alpha.scales");
        List<ItemStack> row = new ArrayList<>();
        for (DragonType t : DragonType.SELECTABLE) {
            row.add(new ItemStack(ModRegistry.ALPHA_SCALES.get(t).get()));
            if (row.size() == 6) {
                items(row, null, INK);
                row = new ArrayList<>();
            }
        }
        if (!row.isEmpty()) items(row, null, INK);
        text("codex.bdragon.alpha.scales.text", DragonAlpha.SCALES_PER_ORB);

        heading("codex.bdragon.alpha.forge");
        DragonType own = ClientDragonData.own().type();
        DragonType race = own == null ? DragonType.FIRE : own;
        add(new Recipe(List.of(new ItemStack(ModRegistry.ALPHA_SCALES.get(race).get(), DragonAlpha.SCALES_PER_ORB), new ItemStack(Items.NETHER_STAR)),
                new ItemStack(ModRegistry.ALPHA_ORBS.get(race).get()), true));
        text("codex.bdragon.alpha.forge.text", DragonAlpha.SCALES_PER_ORB);

        heading("codex.bdragon.alpha.awaken");
        text("codex.bdragon.alpha.awaken.text");

        heading("codex.bdragon.alpha.key");
        text("codex.bdragon.alpha.key.text", key(ClientSetup.KEY_ALPHA_POWER));
        // each alpha in a few words: its look and its power, the rest is for the player to discover
        for (DragonType t : new DragonType[] {DragonType.FOREST, DragonType.STORM, DragonType.NETHER, DragonType.SUNLIGHT, DragonType.MOONLIGHT}) {
            heading(Component.translatable("codex.bdragon.alpha." + t.id()), 0xFF000000 | t.inkColor());
            muted("codex.bdragon.alpha." + t.id() + ".text");
            text("codex.bdragon.alpha." + t.id() + ".power.text");
        }
        gap();
        muted("codex.bdragon.alpha.others");
    }

    private void powersChapter() {
        chapter("codex.bdragon.powers");
        heading("codex.bdragon.powers.breath");
        text("codex.bdragon.powers.breath.text", key(ClientSetup.KEY_BREATH));
        heading("codex.bdragon.powers.roar");
        text("codex.bdragon.powers.roar.text", key(ClientSetup.KEY_ROAR), (int) DragonPlayerManager.ROAR_COST, DragonPlayerManager.ROAR_COOLDOWN / 20);
        heading("codex.bdragon.powers.fly");
        text("codex.bdragon.powers.fly.text");
        heading("codex.bdragon.powers.glide");
        text("codex.bdragon.powers.glide.text");
        heading("codex.bdragon.powers.form");
        text("codex.bdragon.powers.form.text", key(ClientSetup.KEY_FORM));
        heading("codex.bdragon.alpha.key");
        text("codex.bdragon.powers.alpha.text", key(ClientSetup.KEY_ALPHA_POWER), DragonAlpha.POWER_COOLDOWN / 20);
        heading("codex.bdragon.powers.passive");
        text("codex.bdragon.powers.passive.text");
        heading("codex.bdragon.powers.ground");
        text("codex.bdragon.powers.ground.text");
        heading("codex.bdragon.powers.codex");
        text("codex.bdragon.powers.codex.text", key(ClientSetup.KEY_GRIMOIRE));
    }

    private void lairsChapter() {
        chapter("codex.bdragon.lairs");
        text("codex.bdragon.lairs.intro");
        heading("codex.bdragon.lairs.ranks");
        text("codex.bdragon.lairs.ranks.text");
        heading("codex.bdragon.lairs.behaviour");
        text("codex.bdragon.lairs.behaviour.text", Config.LAIR_RESPAWN_DAYS.get());
        heading("codex.bdragon.lairs.loot");
        text("codex.bdragon.lairs.loot.text");
        heading("codex.bdragon.lairs.where");
        for (DragonType type : DragonType.SELECTABLE) {
            text(type.displayName().copy().withStyle(ChatFormatting.BOLD).append(Component.literal(" : ").withStyle(ChatFormatting.RESET))
                    .append(biomes(type)), 0xFF000000 | type.inkColor());
        }
        heading("codex.bdragon.lairs.map");
        text("codex.bdragon.lairs.map.text");

        heading("codex.bdragon.lairs.known");
        var player = Minecraft.getInstance().player;
        List<ModNetwork.LairSync.Entry> list = new ArrayList<>(ClientLairData.current());
        if (player == null || list.isEmpty()) {
            muted("codex.bdragon.lairs.none");
            return;
        }
        double px = player.getX(), pz = player.getZ();
        list.sort(Comparator.comparingDouble(e -> (e.x() - px) * (e.x() - px) + (e.z() - pz) * (e.z() - pz)));
        int shown = 0;
        for (ModNetwork.LairSync.Entry e : list) {
            if (shown++ >= 40) break;
            DragonType type = DragonType.byOrdinal(e.type());
            int dist = (int) Math.sqrt((e.x() - px) * (e.x() - px) + (e.z() - pz) * (e.z() - pz));
            MutableComponent line = Component.literal("• ").append(type.displayName()).append(" · " + DragonHud.roman(e.rank()));
            if (e.defeated()) line.append(Component.translatable("codex.bdragon.lairs.defeated"));
            text(line, e.defeated() ? MUTED : 0xFF000000 | type.inkColor());
            text(Component.translatable("codex.bdragon.lairs.position", dist, LairManager.direction(px, pz, e.x(), e.z()), e.x(), e.z()), MUTED);
        }
    }

    private void itemsChapter() {
        chapter("codex.bdragon.items");
        heading("item.bdragon.dragon_grimoire");
        List<ItemStack> codex = new ArrayList<>();
        for (Item i : DragonRecipes.CODEX) codex.add(new ItemStack(i));
        add(new Recipe(codex, new ItemStack(ModRegistry.GRIMOIRE.get()), true));
        text("codex.bdragon.items.codex", key(ClientSetup.KEY_GRIMOIRE));
        heading("block.bdragon.dragon_altar");
        text("codex.bdragon.items.altar");
        heading("item.bdragon.challenge_horn");
        add(new Recipe(grid(DragonRecipes.HORN), new ItemStack(ModRegistry.CHALLENGE_HORN.get()), false));
        add(new Recipe(List.of(new ItemStack(ModRegistry.DRAGON_SCALE.get()), new ItemStack(ModRegistry.DRAGON_SCALE.get()),
                new ItemStack(Items.BONE)), new ItemStack(ModRegistry.CHALLENGE_HORN.get()), true));
        text("codex.bdragon.items.horn");
        heading("item.bdragon.dragon_scale");
        items(List.of(new ItemStack(ModRegistry.DRAGON_SCALE.get())), Component.translatable("codex.bdragon.items.scale"), INK);
        DragonType own = ClientDragonData.own().type();
        DragonType race = own == null ? DragonType.FIRE : own;
        heading("codex.bdragon.items.alpha_scale.title");
        items(List.of(new ItemStack(ModRegistry.ALPHA_SCALES.get(race).get())), Component.translatable("codex.bdragon.items.alpha_scale"), INK);
        heading("codex.bdragon.items.alpha_orb.title");
        items(List.of(new ItemStack(ModRegistry.ALPHA_ORBS.get(race).get())), Component.translatable("codex.bdragon.items.alpha_orb"), INK);
        heading("codex.bdragon.essences");
        text("codex.bdragon.items.essence");
    }

    private void tipsChapter() {
        chapter("codex.bdragon.tips");
        text("codex.bdragon.tips.text");
    }
}
