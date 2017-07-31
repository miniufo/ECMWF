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

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


public class API {

	static class IgnoreHostVerification implements HostnameVerifier {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}

	static class DummyTrustManager implements X509TrustManager {
		public void checkClientTrusted(X509Certificate[] certs, String authType ) {}
		public void checkServerTrusted(X509Certificate[] certs, String authType ) {}
		public X509Certificate[] getAcceptedIssuers() { return null;}
	}

	private static final int MAX_TRIES = 10;

	public static String VERSION = "1.1";

	public String getUrl() {
		return url;
	}

	private String url;
	private String key;
	private String email;
	private String location;
	private int retry;

	private int offset;
	private int limit;

	private int code;

	private Logger logger;

	{
		try {
			SSLContext ctx = SSLContext.getInstance("SSL");
			ctx.init( null, new TrustManager[]{ new DummyTrustManager()}, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(new IgnoreHostVerification()); 
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public API(String url, String key, String email, Logger logger) {
		this.url = url;
		this.key = key;
		this.email = email;
		this.location = null;
		this.retry = 5;
		this.offset = 0;
		this.limit = 500;
		this.code = 0;
		this.logger = logger;
	}

	private JSONObject _call(String method, String what, Object payload) throws IOException, APIError 
	{
		URL	url;

		if(method.equals("GET")) {
			what += "?offset=" + offset + "&limit=" + limit;
		}

		if (what.startsWith("/"))
			url = new URL(this.url + what);
		else 
			url = new URL(what);


		HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();

		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("X-ECMWF-Key", key);
		connection.setRequestProperty("From", email);
		connection.setRequestMethod(method);

		connection.setInstanceFollowRedirects(true); 

		if(payload != null)
		{
			connection.setDoOutput(true);
			OutputStream out = connection.getOutputStream();
			PrintWriter writer = new PrintWriter(out);
			writer.write(payload.toString());
			writer.flush();
			writer.close();
		}

		code = connection.getResponseCode();


		if(connection.getHeaderField("Location") != null)
			location = connection.getHeaderField("Location");

		if(connection.getHeaderField("Retry-After") != null)
			retry = Integer.parseInt(connection.getHeaderField("Retry-After"));

		InputStream in = null;
		if (code < 400)
			in = connection.getInputStream();
		else
			in = connection.getErrorStream();

		String error = null;
		JSONObject result = null;

		if(code != 204) {
			JSONTokener t = new JSONTokener(in);
			result = (JSONObject) t.nextValue();

			try{
				JSONArray messages =  result.getJSONArray("messages");
				int i = 0;
				while(messages.get(i) != null)
				{
					logger.message(messages.get(i));
					i++;
					offset++;
				}
			}
			catch(JSONException e)
			{
			}

			try{
				error =  result.getString("error");
			}
			catch(JSONException e)
			{
			}

		}

		if(code >= 500)
			throw new APIServerError(code, error);

		if(code >= 400)
			throw new APIClientError(code, error);

		if(error != null)
			throw new APIClientError(error);

		return result;

	}

	public int getCode() {
		return code;
	}

	private JSONObject call(String method, String what, Object payload) throws IOException, APIError 
	{
		int tries = 0;
		while(true)
		{
			try {
				return _call(method, what, payload);
			} catch(APIServerError e)
			{
				if(tries++ >= MAX_TRIES)
					throw e;
				logger.log(e);
				try {
					logger.log("Retrying in one minute.");
					Thread.sleep(60*1000);
				} catch (InterruptedException ignore) {
				}
			}
		}
	}

	private long _transfer(String from,String target,long size) throws IOException, APIError 
	{
		long start = System.currentTimeMillis();
		long total = 0;
		URL url = new URL(from);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestProperty("From", email);
		connection.setInstanceFollowRedirects(true); 

		int code = connection.getResponseCode();

		logger.log("Tranfering " + bytename(size) + " into " + target);
		logger.log("From " + url);


		if(code >= 500)
			throw new APIServerError(code);

		if(code >= 400)
			throw new APIClientError(code);

		if (code == 200) {

			InputStream in = null;
			FileOutputStream out = null;
			try
			{
				in = new BufferedInputStream(connection.getInputStream());
				out = new FileOutputStream(target);

				byte data[] = new byte[64*1024];
				int count;
				while ((count = in.read(data, 0, 1024)) != -1)
				{
					out.write(data, 0, count);
					total += count;
				}
			}
			finally
			{
				if (in != null)
					in.close();
				if (out != null)
					out.close();
			}
		}

		long delta = System.currentTimeMillis() - start;
		if(delta > 0) {
			double elapsed = ((double)total/(double)delta/1000.0) ;
			logger.log("Transfer rate " + bytename(total/elapsed) + "/s");
		}
		return total;
	}

	long transfer(String from, String target, long size) throws IOException, APIError 
	{
		int tries = 0;
		while(true)
		{
			try {
				return _transfer(from, target, size);
			} 
			catch(APIServerError e)
			{
				if(tries++ >= MAX_TRIES)
					throw e;
				logger.log(e);
				try {
					logger.log("Retrying in one minute.");
					Thread.sleep(60*1000);
				} catch (InterruptedException ignore) {
				}
			}
		}
	}

	public JSONObject GET(String what) throws IOException, APIError 
	{
		return call("GET", what, null);
	}

	public JSONObject POST(String what, Object req) throws IOException, APIError 
	{
		return call("POST", what, req);
	}

	public JSONObject DELETE(String what) throws IOException, APIError 
	{
		return call("DELETE", what, null);
	}

	public void sleep() {
		try {
			Thread.sleep(retry*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public String getLocation() {
		return location;
	}


	String bytename(double size)
	{
		String prefix = " KMGTPE";
		int i = 0;
		while (1024 < size){
			size = size / 1024.0;
			i++;
		}
		String p = i >0 ? prefix.substring(i,i+1) : "";
		size = Math.round(size * 100 + 0.5)/100.0;
		return "" + size +  " " + p + "byte" + (size > 1 ? "s" : "");
	}

}
