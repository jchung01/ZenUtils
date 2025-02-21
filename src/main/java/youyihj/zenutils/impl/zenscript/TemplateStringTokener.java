package youyihj.zenutils.impl.zenscript;

import com.google.common.collect.PeekingIterator;
import stanhebben.zenscript.IZenCompileEnvironment;
import stanhebben.zenscript.parser.CompiledDFA;
import stanhebben.zenscript.parser.NFA;
import stanhebben.zenscript.parser.Token;
import stanhebben.zenscript.parser.TokenStream;
import stanhebben.zenscript.util.ZenPosition;
import youyihj.zenutils.api.util.ReflectionInvoked;

import java.io.IOException;

/**
 * @author youyihj
 */
public class TemplateStringTokener extends TokenStream {
    public static final int T_FALLBACK = Integer.MAX_VALUE;
    public static final String T_FULLBACK_REGEX = ".";

    private static CompiledDFA DFA;

    @ReflectionInvoked
    private /* final */ ZenPosition startPosition; // written by asm

    public static void setupDFAFromZenTokener(String[] zenRegexps, int[] zenFinals) {
        String[] regexp = new String[zenRegexps.length - 3];
        int[] finals = new int[zenFinals.length - 3];
        System.arraycopy(zenRegexps, 4, regexp, 0, zenRegexps.length - 4);
        System.arraycopy(zenFinals, 4, finals, 0, zenFinals.length - 4);
        regexp[regexp.length - 1] = T_FULLBACK_REGEX;
        finals[finals.length - 1] = T_FALLBACK;
        DFA = new NFA(regexp, finals).toDFA().optimize().compile();
    }

    public TemplateStringTokener(String data, @SuppressWarnings("unused") ZenPosition startPosition) throws IOException {
        // written by asm
        // this.startPosition = startPosition;
        super(data, DFA);
    }

    public static TemplateStringTokener create(String data, ZenPosition startPosition) {
        try {
            return new TemplateStringTokener(data, startPosition);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public LiteralTokener toLiteral(IZenCompileEnvironment environment) {
        return LiteralTokener.create(new PeekingIterator<Token>() {
            @Override
            public Token peek() {
                return TemplateStringTokener.this.peek();
            }

            @Override
            public Token next() {
                return TemplateStringTokener.this.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean hasNext() {
                return TemplateStringTokener.this.hasNext();
            }
        }, environment);
    }

    @Override
    protected Token process(Token token) {
        if (token == null) {
            return new Token("", -1, startPosition);
        }
        ZenPosition position = token.getPosition();
        int offsetLine = position.getLine() + startPosition.getLine() - 1;
        int offsetLineOffset = position.getLine() == 1 ? position.getLineOffset() : position.getLineOffset() + startPosition.getLineOffset() - 1;
        ZenPosition offsetPosition = new ZenPosition(position.getFile(), offsetLine, offsetLineOffset, position.getFileName());
        return new Token(token.getValue(), token.getType(), offsetPosition);
    }
}
