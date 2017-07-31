package org.ecmwf;

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

public class APIError extends Exception {

	private static final long serialVersionUID = 1L;
	private int code;
	private String error;

	APIError(int code, String error) 
	{
		this.code = code;
		this.error = error;
	}

	APIError(int code) 
	{
		this(code, null);
	}

	APIError(String error) 
	{
		this(0, error);
	}

	public String toString()
	{
		return "APIError code=" + code + ", error=" + error;
	}
}

