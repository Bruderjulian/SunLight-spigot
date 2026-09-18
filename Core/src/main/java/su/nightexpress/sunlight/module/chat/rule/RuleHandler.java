package su.nightexpress.sunlight.module.chat.rule;

import org.jetbrains.annotations.NotNull;

public class RuleHandler {

    private final TrieNode node;
    private final RuleValidator validator;
    private final RuleResult result;

    public RuleHandler(RuleValidator validator, RuleResult result) {
        this.node = new TrieNode();
        this.validator = validator;
        this.result = result;
    }

    public boolean canHandle(String rule) {
        return this.validator.detect(rule);
    }

    public String clean(String rule) {
        return this.validator.clean(rule);
    }

    public void addWord(String word) {
        this.node.add(this.validator.prepare(word));
    }

    public RuleResult scan(String input) {
        if (this.validator.matches(this.node, input)) {
            return this.result;
        }

        return RuleResult.NONE;
    }
}
