package com.intellij.tapestry.psi;

import com.intellij.lexer.XHtmlLexer;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexey Chmutov
 */
public class TmlHighlightingLexer extends XHtmlLexer {

  private static final TokenSet CUSTOM_ATTRIBUTE_TOKENS = TokenSet.create(TelTokenTypes.TAP5_EL_CONTENT);

  public TmlHighlightingLexer() {
    // XHtmlHighlightingLexer was removed in 2026.2; XHtmlLexer in highlight mode is its replacement.
    // Being a BaseHtmlLexer, it drives HtmlEmbeddedContentSupport (TmlEmbeddedContentSupport) so the
    // Tapestry-EL content is sub-lexed with TelLexer for highlighting.
    super(TmlLexer.createElAwareXmlLexer(), true);
  }

  @Override
  protected @NotNull TokenSet createAttributeEmbedmentTokenSet() {
    return TokenSet.orSet(super.createAttributeEmbedmentTokenSet(), CUSTOM_ATTRIBUTE_TOKENS);
  }
}
