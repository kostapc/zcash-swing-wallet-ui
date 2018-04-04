package com.vaklinov.zcashui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class ResourceBundleUTF8 extends ResourceBundle {

	private static ResourceBundleUTF8 instance = null;
	private final String bundleFile = "kotoswing";
	private final static String propertyFile = "koto.properites";
	private static String language = "Default";
			
	private static ResourceBundle.Control UTF8_ENCODING_CONTROL = new ResourceBundle.Control() {
		public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
				boolean reload) throws IllegalAccessException, InstantiationException, IOException {
			Locale myLocale = locale; 
			String lang = getLang();
			if (!lang.equals("Default"))
				myLocale = new Locale(language);
			String bundleName = toBundleName(baseName, myLocale);
			String resourceName = toResourceName(bundleName, "properties");

			InputStream is = loader.getResourceAsStream(resourceName);
			InputStreamReader isr = new InputStreamReader(is, "UTF-8");
			BufferedReader reader = new BufferedReader(isr);
			return new PropertyResourceBundle(reader);
		}
	};

	private ResourceBundleUTF8() {
		Locale myLocale = Locale.getDefault(); 
		String lang = getLang();
		if (!lang.equals("Default"))
			myLocale = new Locale(language);
		try {
			ResourceBundle bundle = ResourceBundle.getBundle(bundleFile, myLocale, UTF8_ENCODING_CONTROL);
			super.setParent(bundle);
		} catch (MissingResourceException e) {
			Log.info("Cannot load resource file: " + myLocale.getLanguage());
		}
	}

	public static ResourceBundleUTF8 getResourceBundle() {
		if (instance == null) {
			instance = new ResourceBundleUTF8();
		}
		return instance;
	}

	@Override
	protected Object handleGetObject(String key) {
		return super.parent.getObject(key);
	}

	@Override
	public Enumeration<String> getKeys() {
		return super.parent.getKeys();
	}

	public String S(String key) {
		if (super.parent == null) {
			return key;
		}
		String str = null;
		try {
			str = super.getString(key);
		} catch (MissingResourceException e) {
			str = key;
		}
		return str;
	}
	
	public static void setLang(String lang) {
		String file = null;
		try {
			file = OSUtil.getProgramDirectory() + File.separator + propertyFile;
	        Properties properties = new Properties();
	        properties.setProperty("language", lang);
	        properties.store(new FileOutputStream(file), "language");
		} catch (IOException e) {
			System.err.println("Cannot create file: " + file);
			e.printStackTrace();
		}
	}

	public static String getLang() {
		String lang = "Default";
		try {
			String file = OSUtil.getProgramDirectory() + File.separator + propertyFile;
			if (!new File(file).exists())
				return lang;
	        Properties properties = new Properties();
	        properties.load(new FileInputStream(file));
	        lang = properties.getProperty("language", "Default");
	        language = lang;
		} catch (IOException e) {
			e.printStackTrace();
		}
        return lang;
	}
}
