package dev.autobuilder.gui;

import dev.autobuilder.schematic.Schematic;
import dev.autobuilder.schematic.SchematicManager;
import dev.autobuilder.util.TextConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.io.File;
import java.util.List;

public class SchematicManagerScreen extends Screen {
    private final Screen parent;
    private List<File> files;
    private int selectedIndex = -1;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 5;
    private TextFieldWidget nameField;

    public SchematicManagerScreen(Screen parent) {
        super(Text.literal("Schematic Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshFiles();
        int centerX = this.width / 2;
        int listY = 40;
        int btnW = 280;
        int btnH = 20;

        // Render file list as buttons
        int total = files.size();
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, total);

        for (int i = startIndex; i < endIndex; i++) {
            final int index = i;
            File f = files.get(i);
            boolean isSelected = (index == selectedIndex);
            String label = (isSelected ? "§b▶ " : "§7• ") + f.getName();

            this.addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {
                this.selectedIndex = index;
                this.clearAndInit();
            }).dimensions(centerX - 140, listY + (i - startIndex) * 22, btnW, btnH).build());
        }

        // Pagination buttons
        if (page > 0) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("◀ Prev"), btn -> {
                page--;
                this.clearAndInit();
            }).dimensions(centerX - 140, listY + ITEMS_PER_PAGE * 22 + 4, 80, 18).build());
        }

        if (endIndex < total) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Next ▶"), btn -> {
                page++;
                this.clearAndInit();
            }).dimensions(centerX + 60, listY + ITEMS_PER_PAGE * 22 + 4, 80, 18).build());
        }

        // Text field for saving
        nameField = new TextFieldWidget(this.textRenderer, centerX - 140, this.height - 65, 140, 20, Text.literal("Name"));
        nameField.setPlaceholder(Text.literal("New schematic name..."));
        this.addDrawableChild(nameField);

        // Save Current button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save Current"), btn -> {
            String name = nameField.getText().trim();
            Schematic active = SchematicManager.getInstance().getActiveSchematic();
            if (!name.isEmpty() && active != null) {
                SchematicManager.getInstance().saveAsync(active, name).thenRun(() -> {
                    if (client != null) {
                        client.execute(this::refreshFiles);
                    }
                });
            }
        }).dimensions(centerX + 5, this.height - 65, 135, 20).build());

        // Load button
        ButtonWidget loadBtn = ButtonWidget.builder(Text.literal("Load Selected"), btn -> {
            if (selectedIndex >= 0 && selectedIndex < files.size()) {
                File file = files.get(selectedIndex);
                SchematicManager.getInstance().loadAsync(file).thenAccept(schem -> {
                    if (client != null && client.player != null) {
                        schem.setOrigin(client.player.getBlockPos());
                        client.player.sendMessage(TextConstants.success("Loaded schematic: " + schem.getName()), false);
                    }
                    if (client != null) {
                        client.execute(() -> client.setScreen(parent));
                    }
                });
            }
        }).dimensions(centerX - 140, this.height - 40, 90, 20).build();
        loadBtn.active = selectedIndex >= 0 && selectedIndex < files.size();
        this.addDrawableChild(loadBtn);

        // Delete button
        ButtonWidget deleteBtn = ButtonWidget.builder(Text.literal("Delete"), btn -> {
            if (selectedIndex >= 0 && selectedIndex < files.size()) {
                File file = files.get(selectedIndex);
                SchematicManager.getInstance().deleteFile(file);
                selectedIndex = -1;
                refreshFiles();
            }
        }).dimensions(centerX - 45, this.height - 40, 90, 20).build();
        deleteBtn.active = selectedIndex >= 0 && selectedIndex < files.size();
        this.addDrawableChild(deleteBtn);

        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> {
            if (client != null) client.setScreen(parent);
        }).dimensions(centerX + 50, this.height - 40, 90, 20).build());
    }

    private void refreshFiles() {
        this.files = SchematicManager.getInstance().listFiles();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§b§lSchematic Manager"), centerX, 15, 0xFFFFFFFF);

        if (files == null || files.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§8No schematics found in directory"), centerX, 80, 0xFFAAAAAA);
        }
    }
}
