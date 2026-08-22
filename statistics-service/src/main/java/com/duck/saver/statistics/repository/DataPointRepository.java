package com.duck.saver.statistics.repository;

import com.duck.saver.statistics.domain.timeseries.DataPoint;
import com.duck.saver.statistics.domain.timeseries.DataPointId;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataPointRepository extends CrudRepository<DataPoint, DataPointId> {

	@Query("{ '_id.account': ?0 }")
	List<DataPoint> findByIdAccount(String account);

}
