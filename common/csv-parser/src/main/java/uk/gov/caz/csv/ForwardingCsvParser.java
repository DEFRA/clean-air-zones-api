package uk.gov.caz.csv;

import com.google.common.base.Preconditions;
import com.opencsv.ICSVParser;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import java.io.IOException;
import java.util.Locale;
import uk.gov.caz.csv.exception.CsvParseException;

/**
 * An abstract base class for all CSV parsers that implements the decorator pattern.
 *
 * <p>All subclasses need to provide the implementation for two methods: {@link
 * ForwardingCsvParser#afterParseLine(java.lang.String[])} and {@link
 * ForwardingCsvParser#beforeParseLine(java.lang.String)}. Those act as hook methods after and
 * before the line is parsed by the delegate, so it is a good place for validation logic. If
 * validation fails, a subclass of {@link CsvParseException} *SHOULD* be thrown for {@link
 * CsvParseExceptionResolver} to handle it.</p>
 */
public abstract class ForwardingCsvParser implements ICSVParser {

  private final ICSVParser delegate;

  public ForwardingCsvParser(ICSVParser delegate) {
    Preconditions.checkNotNull(delegate, "Delegate cannot be null");
    this.delegate = delegate;
  }

  @Override
  public char getSeparator() {
    return delegate.getSeparator();
  }

  @Override
  public char getQuotechar() {
    return delegate.getQuotechar();
  }

  @Override
  public boolean isPending() {
    return delegate.isPending();
  }

  @Override
  public final String[] parseLineMulti(String nextLine) throws IOException {
    beforeParseLine(nextLine);
    String[] result = delegate.parseLineMulti(nextLine);
    afterParseLine(result);
    return result;
  }

  protected abstract void afterParseLine(String[] result);

  protected abstract void beforeParseLine(String nextLine);

  @Override
  public String[] parseLine(String nextLine) throws IOException {
    return delegate.parseLine(nextLine);
  }

  @Override
  public String parseToLine(String[] values, boolean applyQuotesToAll) {
    return delegate.parseToLine(values, applyQuotesToAll);
  }

  @Override
  public CSVReaderNullFieldIndicator nullFieldIndicator() {
    return delegate.nullFieldIndicator();
  }

  @Override
  public String getPendingText() {
    return delegate.getPendingText();
  }

  @Override
  public void setErrorLocale(Locale locale) {
    delegate.setErrorLocale(locale);
  }
}
