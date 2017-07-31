/*
 *
 * (C) Copyright 2012-2013 ECMWF.
 *
 * This software is licensed under the terms of the Apache Licence Version 2.0
 * which can be obtained at http://www.apache.org/licenses/LICENSE-2.0. 
 * In applying this licence, ECMWF does not waive the privileges and immunities 
 * granted to it by virtue of its status as an intergovernmental organisation nor
 * does it submit to any jurisdiction.
 *
 */

package org.ecmwf;

import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.io.File;

import org.json.JSONObject;
import org.json.JSONTokener;

class Retriever implements Logger {

	SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private API api;

	public Retriever(String url, String key, String email)
	{
		api = new API(url, key, email, this);
	}

	public Retriever()
	{
		String url = null;
		String key  = null;
		String email = null;

		Map<String, String> env = System.getenv();
		String home = env.get("HOME");
		File rc = new File(home,".ecmwfapirc");

		if(rc.exists()) {
			try {
				JSONTokener t = new JSONTokener(new FileReader(rc));
				JSONObject result = (JSONObject) t.nextValue();
				try {url = result.getString("url");} catch(Exception e) { log(e); }
				try {key = result.getString("key");} catch(Exception e) { log(e); }
				try {email = result.getString("email");} catch(Exception e) { log(e); }
			}
			catch(Exception e)
			{
				log(e);
			}
		}

		if(url == null) url = "https://api.ecmwf.int/v1";
		if(env.get("ECMWF_API_KEY") != null) key = env.get("ECMWF_API_KEY");
		if(env.get("ECMWF_API_URL") != null) url = env.get("ECMWF_API_URL");
		if(env.get("ECMWF_API_EMAIL") != null) email = env.get("ECMWF_API_EMAIL");

		api = new API(url, key, email, this);
	}

	public void retrieve(String resource, Object req, String target) throws IOException, APIError 
	{
		String status = "";
		log("ECMWF API java library version " + API.VERSION);
		log("ECMWF API at " + api.getUrl());
		JSONObject o;
		o = api.GET("/who-am-i");
		String name = o.getString("full_name");
		if ("".equals(name)) {
			name = "user '" + o.getString("uid")  + "'";
		}

		log("Welcome " + name);

		o = api.GET(resource + "/news");
		try {
			String[] news = o.getString("news").split("\n");
			for(int i = 0; i < news.length; i++)
				log(news[i]);
		}
		catch(Exception e)
		{
		}

		o = api.POST(resource + "/requests", req);
		String location = api.getLocation();
		int code = api.getCode();
		String last = location;
		if(!o.getString("status").equals(status)) {
			status = o.getString("status");
			log("Request is " + status);
		}
		while(code == 202) {
			api.sleep();
			o = api.GET(location);
			last = location;
			location = api.getLocation();
			code = api.getCode();
			if(!o.getString("status").equals(status)) {
				status = o.getString("status");
				log("Request is " + status);
			}
		}

		if(code == 303) {
			long size = o.getInt("size");
			long total = api.transfer(api.getLocation(),target, size);
			if(total != size)
				throw new APIServerError("Size mismatch " + size + " != " + total);

		}

		try{
			api.DELETE(last);
		}
		catch(Exception e)
		{

		}

		log("Done.");
	}


	@Override
	public void log(Object object) {
		String date = FORMAT.format(new Date());
		System.out.println(date + " " + object);

	}

	@Override
	public void message(Object object) {
		System.out.println(object);
	}

}
