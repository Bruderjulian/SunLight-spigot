package su.nightexpress.sunlight.moduleImpl.voting.dialog;

import su.nightexpress.nightcore.ui.dialog.wrap.DialogKey;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteDraft;

public class VoteWizardDialogKeys {

    public static final DialogKey<VoteDraft> WIZARD_KEY         = new DialogKey<>("voting_wizard_key");
    public static final DialogKey<VoteDraft> WIZARD_QUESTION    = new DialogKey<>("voting_wizard_question");
    public static final DialogKey<VoteDraft> WIZARD_DESCRIPTION = new DialogKey<>("voting_wizard_description");
    public static final DialogKey<VoteDraft> WIZARD_DURATION    = new DialogKey<>("voting_wizard_duration");
    public static final DialogKey<VoteDraft> WIZARD_OPTION      = new DialogKey<>("voting_wizard_option");

    private VoteWizardDialogKeys() {
    }
}
