package io.github.kanhiyaog1997.sdet.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.testng.AllureTestNg;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.testng.annotations.Test;

/** Installation of the masking lifecycle and its fail-closed check of the listener order. */
public class AllureLifecycleInstallerTest {

  private static final String INSTALLER = AllureLifecycleInstaller.class.getName();
  private static final String ALLURE = AllureTestNg.class.getName();

  @Test
  public void installsOneLifecycleForTheWholeJvmAndMakesItCurrent() {
    AllureLifecycle installed = AllureLifecycleInstaller.install();

    new AllureLifecycleInstaller();

    assertThat(AllureLifecycleInstaller.install()).isSameAs(installed);
    assertThat(AllureLifecycleInstaller.installed()).isSameAs(installed);
    assertThat(Allure.getLifecycle()).isSameAs(installed);
  }

  @Test
  public void thisModuleRegistersTheInstallerBeforeAllureTestNg() {
    List<String> listeners =
        AllureLifecycleInstaller.registeredListeners(getClass().getClassLoader());

    assertThat(listeners).contains(INSTALLER, ALLURE);
    assertThat(listeners.indexOf(INSTALLER)).isLessThan(listeners.indexOf(ALLURE));
    assertThatCode(() -> AllureLifecycleInstaller.requireRegisteredBeforeAllureTestNg(listeners))
        .doesNotThrowAnyException();
  }

  @Test
  public void acceptsTheInstallerBeforeAllureTestNgOrAllureTestNgAbsent() {
    assertThatCode(
            () -> {
              AllureLifecycleInstaller.requireRegisteredBeforeAllureTestNg(
                  List.of("x.Other", INSTALLER, "y.Other", ALLURE));
              AllureLifecycleInstaller.requireRegisteredBeforeAllureTestNg(List.of(INSTALLER));
              AllureLifecycleInstaller.requireRegisteredBeforeAllureTestNg(List.of());
            })
        .doesNotThrowAnyException();
  }

  @Test
  public void failsClosedWhenAllureTestNgWouldBeConstructedFirst() {
    assertThatThrownBy(
            () ->
                AllureLifecycleInstaller.requireRegisteredBeforeAllureTestNg(
                    List.of(ALLURE, INSTALLER)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(ALLURE + " is registered before " + INSTALLER)
        .hasMessageContaining("without masking");
  }

  @Test
  public void failsClosedWhenAllureTestNgIsRegisteredWithoutTheInstaller() {
    assertThatThrownBy(
            () -> AllureLifecycleInstaller.requireRegisteredBeforeAllureTestNg(List.of(ALLURE)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void readsRegistrationsInClassPathOrderIgnoringCommentsAndBlankLines() throws IOException {
    Path first = serviceDirectory("# comment only\n\n  a.First  # trailing comment\nb.Second\n");
    Path second = serviceDirectory("c.Third\n");

    try (URLClassLoader loader =
        new URLClassLoader(new URL[] {first.toUri().toURL(), second.toUri().toURL()}, null)) {
      assertThat(AllureLifecycleInstaller.registeredListeners(loader))
          .containsExactly("a.First", "b.Second", "c.Third");
    }
  }

  private static Path serviceDirectory(String content) throws IOException {
    Path root = Files.createTempDirectory(Path.of("target"), "services-");
    Path file = root.resolve(AllureLifecycleInstaller.SERVICE_FILE);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return root;
  }
}
