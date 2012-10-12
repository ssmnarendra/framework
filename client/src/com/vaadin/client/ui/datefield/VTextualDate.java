/*
 * Copyright 2011 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.client.ui.datefield;

import java.util.Date;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.TextBox;
import com.vaadin.client.Focusable;
import com.vaadin.client.LocaleNotLoadedException;
import com.vaadin.client.LocaleService;
import com.vaadin.client.VConsole;
import com.vaadin.client.ui.Field;
import com.vaadin.client.ui.SubPartAware;
import com.vaadin.client.ui.textfield.VTextField;
import com.vaadin.shared.EventId;
import com.vaadin.shared.ui.datefield.Resolution;

public class VTextualDate extends VDateField implements Field, ChangeHandler,
        Focusable, SubPartAware {

    private static final String PARSE_ERROR_CLASSNAME = "-parseerror";

    protected final TextBox text;

    protected String formatStr;

    protected boolean lenient;

    private static final String CLASSNAME_PROMPT = "prompt";
    protected static final String ATTR_INPUTPROMPT = "prompt";
    protected String inputPrompt = "";
    private boolean prompting = false;

    public VTextualDate() {
        super();
        text = new TextBox();
        text.addChangeHandler(this);
        text.addFocusHandler(new FocusHandler() {
            @Override
            public void onFocus(FocusEvent event) {
                text.addStyleName(VTextField.CLASSNAME + "-"
                        + VTextField.CLASSNAME_FOCUS);
                if (prompting) {
                    text.setText("");
                    setPrompting(false);
                }
                if (getClient() != null
                        && getClient().hasEventListeners(VTextualDate.this,
                                EventId.FOCUS)) {
                    getClient()
                            .updateVariable(getId(), EventId.FOCUS, "", true);
                }
            }
        });
        text.addBlurHandler(new BlurHandler() {
            @Override
            public void onBlur(BlurEvent event) {
                text.removeStyleName(VTextField.CLASSNAME + "-"
                        + VTextField.CLASSNAME_FOCUS);
                String value = getText();
                setPrompting(inputPrompt != null
                        && (value == null || "".equals(value)));
                if (prompting) {
                    text.setText(readonly ? "" : inputPrompt);
                }
                if (getClient() != null
                        && getClient().hasEventListeners(VTextualDate.this,
                                EventId.BLUR)) {
                    getClient().updateVariable(getId(), EventId.BLUR, "", true);
                }
            }
        });
        add(text);
    }

    protected void updateStyleNames() {
        if (text != null) {
            text.setStyleName(VTextField.CLASSNAME);
            text.addStyleName(getStylePrimaryName() + "-textfield");
        }
    }

    protected String getFormatString() {
        if (formatStr == null) {
            if (currentResolution == Resolution.YEAR) {
                formatStr = "yyyy"; // force full year
            } else {

                try {
                    String frmString = LocaleService
                            .getDateFormat(currentLocale);
                    frmString = cleanFormat(frmString);
                    // String delim = LocaleService
                    // .getClockDelimiter(currentLocale);
                    if (currentResolution.getCalendarField() >= Resolution.HOUR
                            .getCalendarField()) {
                        if (dts.isTwelveHourClock()) {
                            frmString += " hh";
                        } else {
                            frmString += " HH";
                        }
                        if (currentResolution.getCalendarField() >= Resolution.MINUTE
                                .getCalendarField()) {
                            frmString += ":mm";
                            if (currentResolution.getCalendarField() >= Resolution.SECOND
                                    .getCalendarField()) {
                                frmString += ":ss";
                            }
                        }
                        if (dts.isTwelveHourClock()) {
                            frmString += " aaa";
                        }

                    }

                    formatStr = frmString;
                } catch (LocaleNotLoadedException e) {
                    // TODO should die instead? Can the component survive
                    // without format string?
                    VConsole.error(e);
                }
            }
        }
        return formatStr;
    }

    /**
     * Updates the text field according to the current date (provided by
     * {@link #getDate()}). Takes care of updating text, enabling and disabling
     * the field, setting/removing readonly status and updating readonly styles.
     * 
     * TODO: Split part of this into a method that only updates the text as this
     * is what usually is needed except for updateFromUIDL.
     */
    protected void buildDate() {
        removeStyleName(getStylePrimaryName() + PARSE_ERROR_CLASSNAME);
        // Create the initial text for the textfield
        String dateText;
        Date currentDate = getDate();
        if (currentDate != null) {
            dateText = getDateTimeService().formatDate(currentDate,
                    getFormatString());
        } else {
            dateText = "";
        }

        setText(dateText);
        text.setEnabled(enabled);
        text.setReadOnly(readonly);

        if (readonly) {
            text.addStyleName("v-readonly");
        } else {
            text.removeStyleName("v-readonly");
        }

    }

    protected void setPrompting(boolean prompting) {
        this.prompting = prompting;
        if (prompting) {
            addStyleDependentName(CLASSNAME_PROMPT);
        } else {
            removeStyleDependentName(CLASSNAME_PROMPT);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onChange(ChangeEvent event) {
        if (!text.getText().equals("")) {
            try {
                String enteredDate = text.getText();

                setDate(getDateTimeService().parseDate(enteredDate,
                        getFormatString(), lenient));

                if (lenient) {
                    // If date value was leniently parsed, normalize text
                    // presentation.
                    // FIXME: Add a description/example here of when this is
                    // needed
                    text.setValue(
                            getDateTimeService().formatDate(getDate(),
                                    getFormatString()), false);
                }

                // remove possibly added invalid value indication
                removeStyleName(getStylePrimaryName() + PARSE_ERROR_CLASSNAME);
            } catch (final Exception e) {
                VConsole.log(e);

                addStyleName(getStylePrimaryName() + PARSE_ERROR_CLASSNAME);
                // this is a hack that may eventually be removed
                getClient().updateVariable(getId(), "lastInvalidDateString",
                        text.getText(), false);
                setDate(null);
            }
        } else {
            setDate(null);
            // remove possibly added invalid value indication
            removeStyleName(getStylePrimaryName() + PARSE_ERROR_CLASSNAME);
        }
        // always send the date string
        getClient()
                .updateVariable(getId(), "dateString", text.getText(), false);

        // Update variables
        // (only the smallest defining resolution needs to be
        // immediate)
        Date currentDate = getDate();
        getClient().updateVariable(getId(), "year",
                currentDate != null ? currentDate.getYear() + 1900 : -1,
                currentResolution == Resolution.YEAR && immediate);
        if (currentResolution.getCalendarField() >= Resolution.MONTH
                .getCalendarField()) {
            getClient().updateVariable(getId(), "month",
                    currentDate != null ? currentDate.getMonth() + 1 : -1,
                    currentResolution == Resolution.MONTH && immediate);
        }
        if (currentResolution.getCalendarField() >= Resolution.DAY
                .getCalendarField()) {
            getClient().updateVariable(getId(), "day",
                    currentDate != null ? currentDate.getDate() : -1,
                    currentResolution == Resolution.DAY && immediate);
        }
        if (currentResolution.getCalendarField() >= Resolution.HOUR
                .getCalendarField()) {
            getClient().updateVariable(getId(), "hour",
                    currentDate != null ? currentDate.getHours() : -1,
                    currentResolution == Resolution.HOUR && immediate);
        }
        if (currentResolution.getCalendarField() >= Resolution.MINUTE
                .getCalendarField()) {
            getClient().updateVariable(getId(), "min",
                    currentDate != null ? currentDate.getMinutes() : -1,
                    currentResolution == Resolution.MINUTE && immediate);
        }
        if (currentResolution.getCalendarField() >= Resolution.SECOND
                .getCalendarField()) {
            getClient().updateVariable(getId(), "sec",
                    currentDate != null ? currentDate.getSeconds() : -1,
                    currentResolution == Resolution.SECOND && immediate);
        }

    }

    private String cleanFormat(String format) {
        // Remove unnecessary d & M if resolution is too low
        if (currentResolution.getCalendarField() < Resolution.DAY
                .getCalendarField()) {
            format = format.replaceAll("d", "");
        }
        if (currentResolution.getCalendarField() < Resolution.MONTH
                .getCalendarField()) {
            format = format.replaceAll("M", "");
        }

        // Remove unsupported patterns
        // TODO support for 'G', era designator (used at least in Japan)
        format = format.replaceAll("[GzZwWkK]", "");

        // Remove extra delimiters ('/' and '.')
        while (format.startsWith("/") || format.startsWith(".")
                || format.startsWith("-")) {
            format = format.substring(1);
        }
        while (format.endsWith("/") || format.endsWith(".")
                || format.endsWith("-")) {
            format = format.substring(0, format.length() - 1);
        }

        // Remove duplicate delimiters
        format = format.replaceAll("//", "/");
        format = format.replaceAll("\\.\\.", ".");
        format = format.replaceAll("--", "-");

        return format.trim();
    }

    @Override
    public void focus() {
        text.setFocus(true);
    }

    protected String getText() {
        if (prompting) {
            return "";
        }
        return text.getText();
    }

    protected void setText(String text) {
        if (inputPrompt != null && (text == null || "".equals(text))) {
            text = readonly ? "" : inputPrompt;
            setPrompting(true);
        } else {
            setPrompting(false);
        }

        this.text.setText(text);
    }

    private final String TEXTFIELD_ID = "field";

    @Override
    public Element getSubPartElement(String subPart) {
        if (subPart.equals(TEXTFIELD_ID)) {
            return text.getElement();
        }

        return null;
    }

    @Override
    public String getSubPartName(Element subElement) {
        if (text.getElement().isOrHasChild(subElement)) {
            return TEXTFIELD_ID;
        }

        return null;
    }

}
