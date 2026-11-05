package io.github.kanhiyaog1997.sdet.reporting;

import static io.github.kanhiyaog1997.sdet.reporting.SensitiveDataMasker.MASK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import org.testng.annotations.Test;

/** Behaviour of value-based and key-based masking, including the four-character minimum. */
public class SensitiveDataMaskerTest {

  private static final String SECRET = "TASK5-SECRET-12345";

  // ---- value-based masking and the four-character minimum (S4) ----

  @Test
  public void secretsOfFourOrMoreCharactersAreMaskedWhereverTheyAppear() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of(SECRET, "abcd"));

    assertThat(masker.mask("value=" + SECRET + " end")).isEqualTo("value=" + MASK + " end");
    assertThat(masker.mask("xabcdx")).isEqualTo("x" + MASK + "x");
  }

  @Test
  public void secretsShorterThanFourCharactersAreNotMaskedByValue() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of("a", "ab", "abc"));

    assertThat(masker.mask("a ab abc cab")).isEqualTo("a ab abc cab");
    assertThat(masker.ignoredShortSecrets()).isEqualTo(3);
  }

  @Test
  public void shortSecretsAreStillCoveredByKeyBasedRules() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of("abc"));

    assertThat(masker.mask("password=abc")).isEqualTo("password=" + MASK);
  }

  @Test
  public void everyRepeatedOccurrenceIsMasked() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of(SECRET));

    String masked = masker.mask(SECRET + "|" + SECRET + "|" + SECRET + SECRET);

    assertThat(masked).isEqualTo(MASK + "|" + MASK + "|" + MASK).doesNotContain(SECRET);
  }

  @Test
  public void overlappingSecretsLeaveNoFragmentBehind() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of("abcdef", "defghi", "abcd"));

    assertThat(masker.mask("x abcdefghi y")).isEqualTo("x " + MASK + " y");
    assertThat(masker.mask("abcd-abcdef")).isEqualTo(MASK + "-" + MASK);
  }

  @Test
  public void selfOverlappingOccurrencesAreMaskedCompletely() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of("aaaa"));

    assertThat(masker.mask("aaaaaa b")).isEqualTo(MASK + " b");
  }

  @Test
  public void multipleSecretsInTheSameContentAreAllMasked() {
    SensitiveDataMasker masker =
        new SensitiveDataMasker(List.of(SECRET, "second-secret", "third:secret"));

    String masked = masker.mask("a=" + SECRET + ", b=second-secret, c=third:secret.");

    assertThat(masked)
        .doesNotContain(SECRET, "second-secret", "third:secret")
        .isEqualTo("a=" + MASK + ", b=" + MASK + ", c=" + MASK + ".");
  }

  @Test
  public void regexSpecialCharactersInSecretsAreMatchedLiterally() {
    String special = "p@$$w.rd*(+)?[x]{1}|^\\";
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of(special));

    assertThat(masker.mask("before " + special + " after")).isEqualTo("before " + MASK + " after");
    assertThat(masker.mask("pX$$wXrd")).isEqualTo("pX$$wXrd");
  }

  @Test
  public void unicodeSecretsAreMasked() {
    String unicode = "пароль-密码-🔑-ключ";
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of(unicode));

    assertThat(masker.mask("ü " + unicode + " ü")).isEqualTo("ü " + MASK + " ü");
  }

  @Test
  public void nullAndEmptyInputAreReturnedUnchanged() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of(SECRET));

    assertThat(masker.mask(null)).isNull();
    assertThat(masker.mask("")).isEmpty();
  }

  @Test
  public void nullAndEmptySecretValuesAreIgnored() {
    SensitiveDataMasker masker = new SensitiveDataMasker(Arrays.asList(null, "", SECRET));

    assertThat(masker.mask("x " + SECRET)).isEqualTo("x " + MASK);
    assertThat(masker.ignoredShortSecrets()).isZero();
  }

  @Test
  public void nullSecretCollectionIsRejected() {
    assertThatThrownBy(() -> new SensitiveDataMasker(null))
        .isInstanceOf(NullPointerException.class);
  }

  // ---- key-based masking ----

  @Test
  public void sensitiveHeaderValuesAreMasked() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of());

    String masked =
        masker.mask(
            "GET /users HTTP/1.1\n"
                + "Authorization: Basic dXNlcjpwYXNz\n"
                + "proxy-authorization: Digest abc\n"
                + "Cookie: session=abc123\n"
                + "Set-Cookie: id=xyz; HttpOnly\n"
                + "X-Api-Key: k-123\n"
                + "Accept: application/json\n");

    assertThat(masked)
        .contains("Authorization: " + MASK)
        .contains("proxy-authorization: " + MASK)
        .contains("Cookie: " + MASK)
        .contains("Set-Cookie: " + MASK)
        .contains("X-Api-Key: " + MASK)
        .contains("Accept: application/json")
        .contains("GET /users HTTP/1.1")
        .doesNotContain("dXNlcjpwYXNz", "abc123", "xyz", "k-123");
  }

  @Test
  public void sensitiveJsonMembersAreMaskedAtAnyDepthAndPositionInAnArray() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of());

    String masked =
        masker.mask(
            "{\"user\":\"alice\",\"password\":\"p\\\"w\",\"nested\":{\"accessToken\":\"t1\","
                + "\"items\":[{\"client_secret\":\"s1\"},{\"apiKey\":42}]},\"count\":3}");

    assertThat(masked)
        .contains("\"user\":\"alice\"", "\"count\":3")
        .contains("\"password\":\"" + MASK + "\"")
        .contains("\"accessToken\":\"" + MASK + "\"")
        .contains("\"client_secret\":\"" + MASK + "\"")
        .contains("\"apiKey\":\"" + MASK + "\"")
        .doesNotContain("p\\\"w", "t1", "s1", "42");
  }

  @Test
  public void malformedJsonIsStillMaskedAsText() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of());

    assertThat(masker.mask("{\"token\": \"abc\", \"broken\": "))
        .isEqualTo("{\"token\": \"" + MASK + "\", \"broken\": ");
  }

  @Test
  public void queryStringsFormBodiesAndKeyValuePairsAreMasked() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of());

    assertThat(masker.mask("https://api.test/login?user=bob&password=hunter22&page=2"))
        .isEqualTo("https://api.test/login?user=bob&password=" + MASK + "&page=2");
    assertThat(masker.mask("grant_type=password&client_secret=s3c&scope=read"))
        .isEqualTo("grant_type=password&client_secret=" + MASK + "&scope=read");
    assertThat(masker.mask("retrying with api_key=abc-123, attempt 2"))
        .isEqualTo("retrying with api_key=" + MASK + ", attempt 2");
  }

  @Test
  public void bearerTokensAreMaskedAnywhere() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of());

    assertThat(masker.mask("sent Bearer eyJhbGciOiJIUzI1NiJ9.e30.sig to the API"))
        .isEqualTo("sent Bearer " + MASK + " to the API");
  }

  @Test
  public void maskingAppliesToFormattedAndMultiLineContent() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of(SECRET));

    String formatted =
        String.format("Response:%n{%n  \"note\" : \"%s\",%n  \"token\" : \"x\"%n}%n", SECRET);

    assertThat(masker.mask(formatted)).doesNotContain(SECRET).doesNotContain("\"x\"");
  }

  // ---- false positives ----

  @Test
  public void ordinaryTextIsNotOverMasked() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of(SECRET, "abcd"));

    List<String> ordinary =
        List.of(
            "The tokenizer splits the sentence into words.",
            "Password policy: at least twelve characters.",
            "Our secretary will reply within 2 business days.",
            "The bearer of this note may enter.",
            "Basic usage is described in the README.",
            "Cookie consent is stored for 30 days.",
            "{\"user\":\"alice\",\"role\":\"admin\",\"count\":3}",
            "https://example.test/search?q=token+bucket&page=2",
            "abc is not a registered secret, and neither is ABCD.");

    for (String text : ordinary) {
      assertThat(masker.mask(text)).as(text).isEqualTo(text);
    }
  }

  @Test
  public void textWithoutAnySecretsIsReturnedAsTheSameInstance() {
    SensitiveDataMasker masker = new SensitiveDataMasker(List.of(SECRET));
    String text = "nothing sensitive here";

    assertThat(masker.mask(text)).isSameAs(text);
  }

  // ---- configuration integration ----

  @Test
  public void forCurrentRunMasksTheSecretsConfiguredForThisRun() {
    assertThat(SensitiveDataMasker.forCurrentRun().mask("x=" + SECRET))
        .as("SECRET_TASK5_FIXTURE is supplied to this module's tests by Surefire")
        .isEqualTo("x=" + MASK);
  }
}
