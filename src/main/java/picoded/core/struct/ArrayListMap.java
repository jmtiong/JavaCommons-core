package picoded.core.struct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import picoded.core.conv.*;

/**
 * Provides a HashMap<K,A ArrayList<V>>,
 * with utility functions to edit records.
 **/
@SuppressWarnings("serial")
public class ArrayListMap<K, V> extends GenericConvertHashMap<K, ArrayList<V>> {
	/**
	 * Blank constructor
	 **/
	public ArrayListMap() {
		super();
	}
	
	/**
	 * Gets the sublist stored for a key.
	 * If it does not exists, it is initiated.
	 *
	 * @param  the key used
	 *
	 * @return Sublist used for the key
	 **/
	protected ArrayList<V> getSubList(K key) {
		ArrayList<V> ret = get(key);
		
		if (ret == null) {
			ret = new ArrayList<V>();
			put(key, ret);
		}
		
		return ret;
	}
	
	/**
	 * Adds to the sublist associated to the key value
	 *
	 * @param  the key used
	 * @param  the value to store
	 **/
	public void addToList(K key, V val) {
		getSubList(key).add(val);
	}
	
	/**
	 * Adds to the sublist associated to the key value,
	 * only if it does not exists (no duplicates)
	 *
	 * @param  the key used
	 * @param  the value to store
	 **/
	public void addToListIfNotExists(K key, V val) {
		ArrayList<V> subList = getSubList(key);
		if (!(subList.contains(val))) {
			subList.add(val);
		}
	}
	
	/**
	 * Returns the map object, type casted
	 * Following native Map/List standard
	 **/
	@SuppressWarnings("unchecked")
	public Map<K, List<V>> standardMap() {
		return (Map<K, List<V>>) (Object) this;
	}
	
	/**
	 * Implments a JSON to string conversion
	 **/
	@Override
	public String toString() {
		return GenericConvert.toString(this);
	}
}
