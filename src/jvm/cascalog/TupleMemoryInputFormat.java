/*
    Copyright 2010 Nathan Marz
 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
 
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
 
    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package cascalog;

import cascading.tuple.Tuple;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.StringUtils;


public class TupleMemoryInputFormat implements InputFormat<TupleWrapper, NullWritable>{

    public static final String TUPLES_PROPERTY = "memory.format.tuples";


    public static class TupleInputSplit implements InputSplit {
        public int numTuples;


        public TupleInputSplit() {
            
        }
        
        public TupleInputSplit(int numTuples) {
            this.numTuples = numTuples;
        }

        public long getLength() throws IOException {
            return numTuples;
        }

        public String[] getLocations() throws IOException {
            return new String[] {};
        }

        public void write(DataOutput d) throws IOException {
            d.writeInt(numTuples);
        }

        public void readFields(DataInput di) throws IOException {
            numTuples = di.readInt();
        }        
    }

    public static class TupleRecordReader implements RecordReader<TupleWrapper, NullWritable> {

        List<Tuple> tuples;
        int pos = 0;

        public TupleRecordReader(List<Tuple> tuples) {
            this.tuples = tuples;
        }

        public boolean next(TupleWrapper k, NullWritable v) throws IOException {
            if(pos>= tuples.size()) return false;
            k.tuple = tuples.get(pos);
            pos++;
            return true;
        }

        public TupleWrapper createKey() {
            return new TupleWrapper();
        }

        public NullWritable createValue() {
            return NullWritable.get();
        }

        public long getPos() throws IOException {
            return pos;
        }

        public void close() throws IOException {
        }

        public float getProgress() throws IOException {
            if(tuples.size()==0) return 1;
            return (float) (pos*1.0 / tuples.size());
        }
        
    }

    public InputSplit[] getSplits(JobConf jc, int i) throws IOException {
        List<Tuple> tuples = (List<Tuple>) getObject(jc, TUPLES_PROPERTY);
        return new InputSplit[] { new TupleInputSplit(tuples.size()) };
    }

    public RecordReader<TupleWrapper, NullWritable> getRecordReader(InputSplit is, JobConf jc, Reporter rprtr) throws IOException {
        return new TupleRecordReader((List<Tuple>) getObject(jc, TUPLES_PROPERTY));
    }


    public static void setObject(JobConf conf, String key, Object o) {
        conf.set(key, StringUtils.byteToHexString(serialize(o)));
    }

    public static Object getObject(JobConf conf, String key) {
        String s = conf.get(key);
        if(s==null) return null;
        byte[] val = StringUtils.hexStringToByte(s);
        return deserialize(val);
    }

    public static byte[] serialize(Object obj) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.close();
            return bos.toByteArray();
        } catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static Object deserialize(byte[] serialized) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
            ObjectInputStream ois = new ObjectInputStream(bis);
            Object ret = ois.readObject();
            ois.close();
            return ret;
        } catch(IOException ioe) {
            throw new RuntimeException(ioe);
        } catch(ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
