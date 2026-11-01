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
import fenyknit.spectralhardcore.HardcoreMode;

public class SpectralHardcoreSettingsScreen extends Screen {

    private final CreateWorldScreen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private Button doneButton;
    private String livesText;
    private String decayText;
    private LinearLayout content;
    private boolean sharedLives;
    private  CycleButton<Boolean> sharedLivesButton;
    private HardcoreMode selectedMode;

    public SpectralHardcoreSettingsScreen(CreateWorldScreen parent) {
        super(Component.literal("Spectral Hardcore"));
        this.parent = parent;

        GameRules rules = parent.getUiState().getGameRules();

        selectedMode = rules.get(SpectralHardcore.HARDCORE_MODE);

        int maxLives = rules.get(SpectralHardcore.MAX_LIVES);
        livesText = Integer.toString(maxLives);

        int heartsLost = rules.get(SpectralHardcore.HEARTS_LOST_PER_DEATH);
        decayText = Integer.toString(heartsLost);

        sharedLives = maxLives > 1 && rules.get(SpectralHardcore.SHARED_LIVES);
    }


    @Override
    protected void init() {

        // Removes previous elements to rebuil the screen clean
        layout.removeChildren();
        layout.addTitleHeader(title, font);

        content = layout.addToContents(LinearLayout.vertical().spacing(Button.DEFAULT_SPACING));
        content.defaultCellSetting().alignHorizontallyCenter();

        content.addChild(
            CycleButton.<HardcoreMode>builder(
                mode -> switch (mode) {
                    case MULTIPLE_LIVES ->
                        Component.translatable("gui.spectral-hardcore.multiple_lives");
                    case DECAYING_HARDCORE ->
                        Component.translatable("gui.spectral-hardcore.decaying_hardcore");
                }, 
                selectedMode
            )
            .withValues(HardcoreMode.values())
            .create(
                Component.translatable("gui.spectral-hardcore.mode"),
                (button, mode) -> {
                    selectedMode = mode;
                    rebuildWidgets();
                }
            )
        );

        LinearLayout row = content.addChild(
            LinearLayout.horizontal().spacing(Button.DEFAULT_SPACING));

        // Aligns the Shared Lives button with the input bellow it's label
        row.defaultCellSetting().alignVerticallyBottom();

        LinearLayout amountColumn = LinearLayout.vertical().spacing(Button.DEFAULT_SPACING);

        Component amountLabel = switch (selectedMode) {
            case MULTIPLE_LIVES ->
                Component.translatable("gamerule.spectral-hardcore.max_lives");
            case DECAYING_HARDCORE ->
                Component.translatable("gui.spectral-hardcore.hearts_lost_per_death");
        };

        amountColumn.addChild(new StringWidget(amountLabel, font));

        EditBox amountInput = amountColumn.addChild(new EditBox(
            font,
            Button.DEFAULT_WIDTH,
            Button.DEFAULT_HEIGHT,
            amountLabel
        ));

        amountInput.setValue(
            selectedMode == HardcoreMode.MULTIPLE_LIVES ? livesText : decayText
        );

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
        amountInput.setResponder(value -> {
            if (selectedMode == HardcoreMode.MULTIPLE_LIVES) {
                livesText = value;
            } else {
                decayText = value;
            }
            
            updateControls();
        });

        updateControls();

        layout.visitWidgets(widget -> addRenderableWidget(widget));
        repositionElements();    
    }

    private void updateControls() {
        Integer amount = getValidAmount();
        boolean multipleLivesMode = selectedMode == HardcoreMode.MULTIPLE_LIVES;
        
        doneButton.active = amount != null;
        sharedLivesButton.active = multipleLivesMode && amount != null && amount > 1;

        if (multipleLivesMode && amount != null && amount == 1) {
            sharedLives = false;
            sharedLivesButton.setValue(false);
        }
    }

    private Integer getValidAmount() {
        String text = selectedMode == HardcoreMode.MULTIPLE_LIVES ? livesText : decayText;
    
        try {
            int amount = Integer.parseInt(text);

            if (amount >= 1) {
                return amount;
            }
        } catch(NumberFormatException exception) {
            return null;
        }
        return null;
    }

    private void onDone() {
        Integer amount = getValidAmount();

        if (amount == null) {
            return;
        }

        GameRules rules = parent.getUiState().getGameRules().copy(
            parent.getUiState().getSettings().dataConfiguration().enabledFeatures()
        );

        rules.set(SpectralHardcore.HARDCORE_MODE, selectedMode, null);
        
        if (selectedMode == HardcoreMode.MULTIPLE_LIVES) {
            rules.set(SpectralHardcore.MAX_LIVES, amount, null);
            rules.set(SpectralHardcore.SHARED_LIVES, amount > 1 && sharedLives, null);
        } else {
            rules.set(SpectralHardcore.HEARTS_LOST_PER_DEATH, amount, null);
            rules.set(SpectralHardcore.SHARED_LIVES, false, null);
        }

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
        content.setY(layout.getHeaderHeight() + Button.DEFAULT_SPACING);
    }
}