package com.duck.saver.statistics.repository.converter;

import com.duck.saver.statistics.domain.timeseries.DataPointId;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class DataPointIdReaderConverter implements Converter<Document, DataPointId> {

	@Override
	public DataPointId convert(Document document) {

		Date date = document.getDate("date");
		String account = document.getString("account");

		return new DataPointId(account, date);
	}
}
