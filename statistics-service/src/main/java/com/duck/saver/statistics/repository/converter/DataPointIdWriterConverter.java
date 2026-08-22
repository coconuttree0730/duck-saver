package com.duck.saver.statistics.repository.converter;

import com.duck.saver.statistics.domain.timeseries.DataPointId;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DataPointIdWriterConverter implements Converter<DataPointId, Document> {

	@Override
	public Document convert(DataPointId id) {

		Document document = new Document();

		document.put("date", id.getDate());
		document.put("account", id.getAccount());

		return document;
	}
}
