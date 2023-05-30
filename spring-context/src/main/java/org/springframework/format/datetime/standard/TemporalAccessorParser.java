/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.format.datetime.standard;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import org.springframework.format.Parser;

/**
 * {@link Parser} implementation for a JSR-310 {@link TemporalAccessor},
 * using a {@link DateTimeFormatter}) (the contextual one, if available).
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see DateTimeContextHolder#getFormatter
 * @see LocalDate#parse(CharSequence, DateTimeFormatter)
 * @see LocalTime#parse(CharSequence, DateTimeFormatter)
 * @see LocalDateTime#parse(CharSequence, DateTimeFormatter)
 * @see ZonedDateTime#parse(CharSequence, DateTimeFormatter)
 * @see OffsetDateTime#parse(CharSequence, DateTimeFormatter)
 * @see OffsetTime#parse(CharSequence, DateTimeFormatter)
 */
public final class TemporalAccessorParser implements Parser<TemporalAccessor> {

	private final Class<? extends TemporalAccessor> temporalAccessorType;

	private final DateTimeFormatter formatter;


	/**
	 * Create a new TemporalAccessorParser for the given TemporalAccessor type.
	 * @param temporalAccessorType the specific TemporalAccessor class
	 * (LocalDate, LocalTime, LocalDateTime, ZonedDateTime, OffsetDateTime, OffsetTime)
	 * @param formatter the base DateTimeFormatter instance
	 */
	public TemporalAccessorParser(Class<? extends TemporalAccessor> temporalAccessorType, DateTimeFormatter formatter) {
		this.temporalAccessorType = temporalAccessorType;
		this.formatter = formatter;
	}


	@Override
	public TemporalAccessor parse(String text, Locale locale) throws ParseException {
		DateTimeFormatter formatterToUse = DateTimeContextHolder.getFormatter(this.formatter, locale);
		if (LocalDate.class == this.temporalAccessorType) {
			return LocalDate.parse(text, formatterToUse);
		}
		else if (LocalTime.class == this.temporalAccessorType) {
			return LocalTime.parse(text, formatterToUse);
		}
		else if (LocalDateTime.class == this.temporalAccessorType) {
			return LocalDateTime.parse(text, formatterToUse);
		}
		else if (ZonedDateTime.class == this.temporalAccessorType) {
			return ZonedDateTime.parse(text, formatterToUse);
		}
		else if (OffsetDateTime.class == this.temporalAccessorType) {
			return OffsetDateTime.parse(text, formatterToUse);
		}
		else if (OffsetTime.class == this.temporalAccessorType) {
			return OffsetTime.parse(text, formatterToUse);
		}
		else {
			throw new IllegalStateException("Unsupported TemporalAccessor type: " + this.temporalAccessorType);
		}
	}

}
