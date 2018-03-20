package picoded.core.file;

//
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import picoded.core.struct.GenericConvertMap;

/**
 * Config file loader
 *
 * Iterates a filepath and loads all the various config files
 *
 * Note that folder, and filename will be used as the config path. Unless its config.ini
 **/
public class ConfigFileSet extends ConfigFile implements GenericConvertMap<String, Object> {
	
	//-----------------------------------------------------------------------------------
	//
	// Internal config file map
	//
	//-----------------------------------------------------------------------------------
	
	/**
	 * The actual internal config file set mapping
	 *
	 * <main, <main-include.test, hello>>
	 * <related <main-include.text, hi>>
	 * <jsonFileName <jsonKey, jsonValue>>
	 **/
	protected Map<String, ConfigFile> configFileMap = new ConcurrentHashMap<String, ConfigFile>();
	
	//-----------------------------------------------------------------------------------
	//
	// Constructors
	//
	//-----------------------------------------------------------------------------------
	
	/**
	 * Blank constructor
	 **/
	public ConfigFileSet() {
		
	}
	
	/**
	 * Constructor with the default file path to scan
	 **/
	public ConfigFileSet(String filePath) {
		addConfigSet(filePath);
	}
	
	/**
	 * Constructor with the default file path to scan
	 **/
	public ConfigFileSet(File filePath) {
		addConfigSet(filePath);
	}
	
	//-----------------------------------------------------------------------------------
	//
	// Config File folder importing
	//
	//-----------------------------------------------------------------------------------

	public ConfigFileSet addConfigSet(File filePath) {
		return addConfigSet(filePath, "", ".");
	}

	public ConfigFileSet addConfigSet(String filePath) {
		return addConfigSet(filePath, "", ".");
	}

	public ConfigFileSet addConfigSet(String filePath, String prefix, String separator) {
		return addConfigSet_recursive(new File(filePath), prefix, separator);
	}

	public ConfigFileSet addConfigSet(File inFile, String prefix, String separator) {
		return addConfigSet_recursive(inFile, prefix, separator);
	}

	private ConfigFileSet addConfigSet_recursive(File inFile, String rootPrefix, String separator) {
		if (rootPrefix == null) {
			rootPrefix = "";
		}

		// file is directory
		if (inFile.isDirectory()) {
			File[] innerFiles = inFile.listFiles();

			// Loop through all the files in the directory
			for (File innerFile : innerFiles) {

				// innerFile is a directory
				if (innerFile.isDirectory()) {
					String parentFolderName = innerFile.getName();
					if (!rootPrefix.isEmpty()) {
						parentFolderName = rootPrefix + separator + parentFolderName;
					}
					// Recursively loop the directory with the parent folder name
					addConfigSet_recursive(innerFile, parentFolderName, separator);
				} else {
					// Add the file to the config file map
					addConfigSet_recursive(innerFile, rootPrefix, separator);
				}
			}
		} else {
			String fileName = inFile.getName();
			
			// Get the filename extension
			int endingDot = fileName.lastIndexOf('.');
			String extension = "";
			if (endingDot > 0) {
				extension = fileName.substring(endingDot + 1);
			}
			
			// Only accept ini or json files
			if (extension.equalsIgnoreCase("json") || extension.equalsIgnoreCase("js") || extension.equalsIgnoreCase("html")) {
				
				ConfigFile cFile = new ConfigFile(inFile);
				fileName = fileName.substring(0, fileName.lastIndexOf('.'));
				String prefix = "";
				if (!rootPrefix.isEmpty()) {
					prefix += rootPrefix + separator;
				}
				
				configFileMap.put(prefix + fileName, cFile);
			}
		}
		
		return this;
	}
	
	//-----------------------------------------------------------------------------------
	//
	// Getting a sub mapping clone
	//
	//-----------------------------------------------------------------------------------
	
	/**
	 * Getting sub map under a prefix, filtering out restricted namespace
	 *
	 * @param  The prefix to filter for. Example ("sys.")
	 *
	 * @return  The created sub map
	 **/
	public ConfigFileSet createSubMap(String prefix) {
		return createSubMapInternal(prefix, null);
	}
	
	/**
	 * Getting sub map under a prefix, filtering out restricted namespace
	 *
	 * @param  The prefix to filter for. Example ("sys.")
	 * @param  The namespace to ignore
	 *
	 * @return  The created sub map
	 **/
	public ConfigFileSet createSubMap(String prefix, String ignore) {
		return createSubMapInternal(prefix, new String[] { ignore });
	}
	
	/**
	 * Getting sub map under a prefix, filtering out restricted namespace
	 *
	 * @param  The prefix to filter for. Example ("sys.")
	 * @param  The namespace to ignore
	 *
	 * @return  The created sub map
	 **/
	public ConfigFileSet createSubMap(String prefix, String... ignore) {
		return createSubMapInternal(prefix, ignore);
	}
	
	/**
	 * Getting sub map under a prefix, filtering out restricted namespace
	 *
	 * @param  The prefix to filter for. Example ("sys.")
	 * @param  The namespace to ignore
	 *
	 * @return  The created sub map
	 **/
	protected ConfigFileSet createSubMapInternal(String prefix, String[] ignore) {
		ConfigFileSet ret = new ConfigFileSet();
		
		// Blank is as good as null
		if (prefix != null && prefix.length() <= 0) {
			prefix = null;
		}
		
		//
		// Iterate across all the ConfigFile items : and populate the result
		//
		for (String key : configFileMap.keySet()) {
			
			//
			// Check for items to ignore
			//
			if (ignore != null) {
				boolean breakOut = false;
				for (String ignorePart : ignore) {
					if (ignorePart != null && key.startsWith(ignorePart)) {
						breakOut = true;
						continue;
					}
				}
				
				if (breakOut == true) {
					continue;
				}
			}
			
			//
			// Add if prefix is null (all valid), or valid
			//
			if (prefix == null) {
				ret.configFileMap.put(key, configFileMap.get(key));
			} else {
				if (key.startsWith(prefix)) {
					
					// Get the subkey without prefix filter
					String subkey = key.substring(prefix.length()).trim();
					
					// The subkey without starting "."
					while (subkey.startsWith(".")) {
						subkey = subkey.substring(1).trim();
					}
					
					// Skip if subkey ends up being blank
					if (subkey.length() <= 0) {
						continue;
					}
					
					// Insert the sub map key without the prefix
					ret.configFileMap.put(subkey, configFileMap.get(key));
				}
			}
		}
		
		if (ret.configFileMap.size() <= 0) {
			return null;
		}
		return ret;
	}
	
	/**
	 * Memoizer cache for getCachedSubMap()
	 **/
	protected Map<String, ConfigFileSet> _subMapCache = new ConcurrentHashMap<String, ConfigFileSet>();
	
	/**
	 * Gets an internally cached submap (with prefix)
	 **/
	protected ConfigFileSet getCachedSubMap(String prefix) {
		if (_subMapCache.containsKey(prefix)) {
			return _subMapCache.get(prefix);
		}
		
		ConfigFileSet cacheObj = createSubMapInternal(prefix, null);
		if (cacheObj != null) {
			_subMapCache.put(prefix, cacheObj);
		}
		return cacheObj;
	}
	
	//-----------------------------------------------------------------------------------
	//
	// KeySet handling
	//
	//-----------------------------------------------------------------------------------
	
	/**
	 * Top layer keySet fetching
	 **/
	public Set<String> keySet() {
		HashSet<String> ret = new HashSet<String>();
		
		//
		// Iterate across all the ConfigFile items : and populate the result
		//
		for (String key : configFileMap.keySet()) {
			String keyString = key.toString();
			String[] splitKeyString = keyString.split("\\.");
			ret.add(splitKeyString[0]);
		}
		
		return ret;
	}
	
	//-----------------------------------------------------------------------------------
	//
	// Get request handling
	//
	//-----------------------------------------------------------------------------------
	
	/**
	 * When a get request is called here, it attempts to pull from the resepective sub map, by splitting the request key.
	 *
	 * For example: config.main.header.test
	 *
	 * Will be split as "config.main.header", and "test",
	 * It will then attempt to fetch from the "config.main.header" file if it exists.
	 *
	 * If it fails to find, it will then resepectively search 1 level higher
	 * Splitting it as followed "config.main", and "header.test"
	 **/
	public Object get(Object key) {
		String keyString = key.toString();
		String[] splitKeyString = keyString.split("\\.");
		
		// an issue could arise if there are conflicting keys
		// example
		// <a.b.c, <d, e>> //json
		// <a.b, <c.d, e>> //ini file
		// in this case, passing a key of "a.b.c.d" will always hit the json
		// file first, which might not be intended.
		// having nonconflicting keys will avoid this, but this is just a heads
		// up
		for (int splitPt = splitKeyString.length; splitPt > 0; --splitPt) {
			String fileKey = StringUtils.join(ArrayUtils.subarray(splitKeyString, 0, splitPt), ".");
			String headerKey = StringUtils.join(
				ArrayUtils.subarray(splitKeyString, splitPt, splitKeyString.length), ".");
			
			Object returnVal = getExact(fileKey, headerKey);
			
			if (returnVal != null) {
				return returnVal;
			}
		}
		
		//
		// Attempts to get a submap if possible, else returns null
		//
		return getCachedSubMap(keyString);
	}
	
	// use this if you know the exact keyvaluepair you want
	public Object getExact(Object fileKey, Object headerKey) {
		String fileKeyString = fileKey.toString();
		String headerKeyString = headerKey.toString();
		
		Map<String, Object> subMap = configFileMap.get(fileKeyString);
		if (subMap != null) {
			if (headerKeyString.length() <= 0) {
				return subMap;
			}
			return subMap.get(headerKeyString);
		}
		return null;
	}
}
