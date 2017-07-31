//
import org.ecmwf.DataServer;
import org.json.JSONObject;


//
public final class Test{
	//
	public static void main(String[] args){
		//for(int year=1979;year<=1980;year++)
		downloadVar(2004,"2t","d:/msl_0.75t.nc");
	}
	
	static void downloadVar(int year,String var,String fname){
		DataServer server = new DataServer();
		
		JSONObject  request = new JSONObject();
		
		request.put("dataset" , "interim");
		request.put("date"    , year+"-09-25/to/"+year+"-09-28");
		request.put("stream"  , "oper");
		request.put("levtype" , "sfc");
		request.put("param"   , var);
		request.put("step"    , "0");
		request.put("time"    , "00:00:00/06:00:00/18:00:00/12:00:00");
		request.put("type"    , "an");
		request.put("area"    , "90/-180/-90/180");
		request.put("grid"    , "0.75/0.75");
		request.put("format"  , "netcdf");
		request.put("target"  , fname);
		
		try{
			server.retrieve(request);
			
		}catch(Exception e){ e.printStackTrace(); System.exit(0);}
	}
}
