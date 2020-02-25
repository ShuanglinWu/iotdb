package org.apache.iotdb.cluster.query.reader;

import java.io.IOException;
import java.util.NoSuchElementException;
import org.apache.iotdb.db.query.reader.series.ManagedSeriesReader;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.TimeValuePair;
import org.apache.iotdb.tsfile.read.common.BatchData;
import org.apache.iotdb.tsfile.read.reader.IPointReader;

public class BatchedPointReader implements ManagedSeriesReader {

  private static final int BATCH_SIZE = 4096;
  private IPointReader innerReader;

  private volatile boolean managedByPool;
  private volatile boolean hasRemaining;

  private BatchData batchData;
  private TSDataType dataType;

  public BatchedPointReader(IPointReader innerReader, TSDataType dataType) {
    this.innerReader = innerReader;
    this.dataType = dataType;
  }

  @Override
  public boolean isManagedByQueryManager() {
    return managedByPool;
  }

  @Override
  public void setManagedByQueryManager(boolean managedByQueryManager) {
    this.managedByPool = managedByQueryManager;
  }

  @Override
  public boolean hasRemaining() {
    return hasRemaining;
  }

  @Override
  public void setHasRemaining(boolean hasRemaining) {
    this.hasRemaining = hasRemaining;
  }

  @Override
  public boolean hasNextBatch() throws IOException {
    if (batchData != null) {
      return true;
    }
    constructBatch();
    return batchData != null;
  }

  private void constructBatch() throws IOException {
    if (hasNext()) {
      batchData = new BatchData(dataType);
      while (hasNext() && batchData.length() < BATCH_SIZE) {
        TimeValuePair next = next();
        batchData.putAnObject(next.getTimestamp(), next.getValue().getValue());
      }
    }
  }

  @Override
  public BatchData nextBatch() throws IOException {
    if (!hasNextBatch()) {
      throw new NoSuchElementException();
    }
    BatchData ret = batchData;
    batchData = null;
    return ret;
  }

  public boolean hasNext() throws IOException {
    return innerReader.hasNextTimeValuePair();
  }

  public TimeValuePair next() throws IOException {
    return innerReader.nextTimeValuePair();
  }

  public TimeValuePair current() throws IOException {
    return innerReader.currentTimeValuePair();
  }

  @Override
  public void close() throws IOException {
    innerReader.close();
  }
}
