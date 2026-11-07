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
    private TextFieldWidget nameField;

    public SchematicManagerScreen(Screen parent) {
        super(Text.literal("Schematic Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshFiles();
        int centerX = this.width / 2;

        nameField = new TextFieldWidget(this.textRenderer, centerX - 150, this.height - 65, 140, 20, Text.literal("Name"));
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
        }).dimensions(centerX + 2, this.height - 65, 148, 20).build());

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
        }).dimensions(centerX - 150, this.height - 40, 95, 20).build();
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
        }).dimensions(centerX - 50, this.height - 40, 95, 20).build();
        deleteBtn.active = selectedIndex >= 0 && selectedIndex < files.size();
        this.addDrawableChild(deleteBtn);

        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> {
            if (client != null) client.setScreen(parent);
        }).dimensions(centerX + 50, this.height - 40, 100, 20).build());
    }

    private void refreshFiles() {
        this.files = SchematicManager.getInstance().listFiles();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int listX = centerX - 150;
        int listY = 40;
        int itemHeight = 16;

        if (files != null) {
            for (int i = 0; i < files.size(); i++) {
                int itemY = listY + i * itemHeight;
                if (mouseX >= listX && mouseX <= listX + 300 && mouseY >= itemY && mouseY < itemY + itemHeight) {
                    this.selectedIndex = i;
                    this.clearAndInit();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§b§lSchematic Manager"), centerX, 15, 0xFFFFFFFF);

        int listX = centerX - 150;
        int listY = 40;
        int itemHeight = 16;
        int listHeight = 120;

        // List background
        context.fill(listX - 2, listY - 2, listX + 302, listY + listHeight + 2, 0x90000000);

        if (files == null || files.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§8No schematics found in directory"), centerX, listY + 50, 0xFFAAAAAA);
        } else {
            for (int i = 0; i < Math.min(files.size(), 7); i++) {
                File f = files.get(i);
                int itemY = listY + i * itemHeight;
                boolean isSelected = (i == selectedIndex);

                if (isSelected) {
                    context.fill(listX, itemY, listX + 300, itemY + itemHeight, 0x6000CED1);
                }

                String prefix = isSelected ? "§b▶ " : "§7• ";
                context.drawTextWithShadow(this.textRenderer, Text.literal(prefix + f.getName()), listX + 6, itemY + 4, 0xFFFFFFFF);
            }
        }
    }
}
