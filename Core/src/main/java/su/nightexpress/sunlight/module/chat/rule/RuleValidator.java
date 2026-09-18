package su.nightexpress.sunlight.module.chat.rule;

public interface RuleValidator {

    boolean detect(String rule);

    String clean(String rule);

    String prepare(String word);

    boolean matches(TrieNode root, String word);

    static RuleValidator forSequence(char prefix) {

        return new RuleValidator() {

            @Override
            public boolean detect(String rule) {
                return rule.charAt(0) == prefix && rule.charAt(rule.length() - 1) == prefix;
            }

            @Override

            public String clean(String rule) {
                return rule.substring(1, rule.length() - 1);
            }

            @Override

            public String prepare(String word) {
                return word;
            }

            @Override
            public boolean matches(TrieNode root, String input) {
                for (int i = 0; i < input.length(); i++) {
                    if (RuleValidator.containsPrefix(root, input, i, 1))
                        return true;
                }
                return false;
            }
        };
    }

    static RuleValidator forPrefix(char prefix) {

        return new RuleValidator() {
            @Override
            public boolean detect(String rule) {
                return rule.charAt(0) == prefix;
            }

            @Override

            public String clean(String rule) {
                return rule.substring(1);
            }

            @Override

            public String prepare(String word) {
                return new StringBuilder(word).reverse().toString();
            }

            @Override
            public boolean matches(TrieNode root, String input) {
                return RuleValidator.containsPrefix(root, input, input.length() - 1, -1);
            }
        };
    }

    static RuleValidator forSuffix(char suffix) {

        return new RuleValidator() {
            @Override
            public boolean detect(String rule) {
                return rule.charAt(rule.length() - 1) == suffix;
            }

            @Override

            public String clean(String rule) {
                return rule.substring(0, rule.length() - 1);
            }

            @Override

            public String prepare(String word) {
                return word;
            }

            @Override
            public boolean matches(TrieNode root, String input) {
                return RuleValidator.containsPrefix(root, input, 0, 1);
            }
        };
    }

    private static boolean containsPrefix(TrieNode root, String input, int start, int step) {
        TrieNode currentNode = root;
        int length = input.length();

        for (int index = start; index >= 0 && index < length; index += step) {
            char letter = input.charAt(index);

            currentNode = currentNode.children(letter);
            if (currentNode == null)
                return false;
            if (currentNode.isEnd())
                return true;
        }

        return false;
    }
}
