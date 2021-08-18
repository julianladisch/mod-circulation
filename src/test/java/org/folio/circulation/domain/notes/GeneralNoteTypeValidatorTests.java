package org.folio.circulation.domain.notes;

import static api.support.matchers.FailureMatcher.hasValidationFailure;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.folio.circulation.support.results.Result;
import org.junit.jupiter.api.Test;

public class GeneralNoteTypeValidatorTests {

  @Test
  public void allowSingleNoteType() {
    GeneralNoteTypeValidator validator = new GeneralNoteTypeValidator();

    final NoteType noteType = generateNoteType();

    Result<Optional<NoteType>> singleNoteType = Result.of(() -> Optional.of(noteType));

    Result<NoteType> result = validator.refuseIfNoteTypeNotFound(singleNoteType);

    assertThat(result.succeeded(), is(true));
    assertThat(result.value(), is(noteType));
  }

  @Test
  public void failWhenNoNoteType() {
    GeneralNoteTypeValidator validator = new GeneralNoteTypeValidator();

    Result<NoteType> result = validator.refuseIfNoteTypeNotFound(Result.of(Optional::empty));

    assertThat(result, hasValidationFailure("No General note type found"));
  }

  private NoteType generateNoteType() {
    return new NoteType(UUID.randomUUID().toString(), "General");
  }
}
