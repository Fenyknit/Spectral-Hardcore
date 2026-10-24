package fenyknit.spectralhardcore.client.screen;

// GUI Imports
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

// Other Imports
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.gamerules.GameRules;
import fenyknit.spectralhardcore.SpectralHardcore;

public class SpectralHardcoreSettingsScreen extends Screen {

    private final CreateWorldScreen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private Button doneButton;
    private String livesText;
    private LinearLayout row;
    private boolean sharedLives;
    private  CycleButton<Boolean> sharedLivesButton;


    public SpectralHardcoreSettingsScreen(CreateWorldScreen parent) {
        super(Component.literal("Spectral Hardcore"));
        this.parent = parent;

        int maxLives = parent.getUiState().getGameRules().get(SpectralHardcore.MAX_LIVES);
        livesText = Integer.toString(maxLives);

        sharedLives = maxLives > 1 
        && 
        parent.getUiState().getGameRules().get(SpectralHardcore.SHARED_LIVES);

    }


    @Override
    protected void init() {

        // Removes previous elements to rebuil the screen clean
        layout.removeChildren();
        layout.addTitleHeader(title, font);

        row = layout.addToContents(LinearLayout.horizontal().spacing(Button.DEFAULT_SPACING));
        // Aligns the Shared Lives button with the input bellow it's label
        row.defaultCellSetting().alignVerticallyBottom();

        LinearLayout amountColumn = LinearLayout.vertical().spacing(Button.DEFAULT_SPACING);

        amountColumn.addChild(new StringWidget(
            Component.translatable("gamerule.spectral-hardcore.max_lives"), 
            font
        ));

        EditBox livesInput = amountColumn.addChild(new EditBox(
            font,
            Button.DEFAULT_WIDTH,
            Button.DEFAULT_HEIGHT,
            Component.translatable("gamerule.spectral-hardcore.max_lives")
        ));

        livesInput.setValue(livesText);
        sharedLivesButton = row.addChild(
            CycleButton.onOffBuilder(sharedLives).create(Component.translatable("gamerule.spectral-hardcore.shared_lives"),
            (button, enabled) -> sharedLives = enabled
        ));

        row.addChild(amountColumn);

        LinearLayout footer = layout.addToFooter(
                LinearLayout.horizontal().spacing(Button.DEFAULT_SPACING)
        );

        doneButton = footer.addChild(
            Button.builder(
                Component.translatable("gui.done"),
                button -> onDone()
            ).build()
        );

        footer.addChild(
            Button.builder(
                Component.translatable("gui.cancel"),
                button -> onClose()
            ).build()
        );
        // Updates the buttons when editing, and values changes are only saved when on Done.
        livesInput.setResponder(value -> {
            livesText = value;
            updateControls();
        });

        updateControls();

        layout.visitWidgets(widget -> addRenderableWidget(widget));
        repositionElements();    
    }

    private void updateControls() {
        Integer maxLives = getValidMaxLives();

        doneButton.active = maxLives != null;
        sharedLivesButton.active = maxLives != null && maxLives > 1;

        if (maxLives != null && maxLives == 1) {
            sharedLives = false;
            sharedLivesButton.setValue(false);
        }
    }

    private Integer getValidMaxLives() {
        try {
            int maxLives = Integer.parseInt(livesText);

            if (maxLives >= 1) {
                return maxLives;
            }
        } catch (NumberFormatException exception) {
            return null;
        }
        
        return null;
    }

    private void onDone() {
        Integer maxLives = getValidMaxLives();

        if (maxLives == null) {
            return;
        }

        GameRules rules = parent.getUiState().getGameRules().copy(
            parent.getUiState().getSettings().dataConfiguration().enabledFeatures()
        );

        rules.set(SpectralHardcore.MAX_LIVES, maxLives, null);
        rules.set(SpectralHardcore.SHARED_LIVES, maxLives > 1 && sharedLives, null);
        
        parent.getUiState().setGameRules(rules);

        onClose();
    }
    
    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        row.setY(layout.getHeaderHeight() + Button.DEFAULT_SPACING);
    }
}