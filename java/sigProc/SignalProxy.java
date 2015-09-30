package nl.dcc.buffer_bci;
import java.io.IOException;
import java.util.Random;
import nl.fcdonders.fieldtrip.bufferclient.BufferClient;
import nl.fcdonders.fieldtrip.bufferclient.Header;

public class SignalProxy {
	static int VERB=1;

	 boolean run = true;
	 final String hostname;
	 final int port;
	 final int nChannels;
	 final double fSample;
	 final int blockSize;
	 final BufferClient client;
	 final Random generator;
	 final double sinFreq = 10;
	 int nSample=0;	 
	 static final String usage=
		    "Usage: SignalProxy buffhost:buffport fsample nchans buffrate\n"
		  + "where:\n"
		  + "\t buffersocket\t is a string of the form bufferhost:bufferport (localhost:1972)\n"
		  + "\t fsample\t is the frequency data is generated in Hz                 (100)\n"
		  + "\t nchans\t is the number of simulated channels to make                 (3)\n"
		  + "\t buffrate\t is the frequency in Hz that data is sent to the buffer   (50)\n";

	 public static void main(String[] args) throws IOException,InterruptedException {
		   String hostname="localhost";
			int port=1972;
			int nChannels=4;
			double fSample=100;
			int blockSize=1;
		  
		if (args.length==0 ){
			 System.out.print(usage);
			 System.exit(-1);
		}
		if (args.length>=1) {
			hostname = args[0];
			int sep = hostname.indexOf(':');
			if ( sep>0 ) {
				 port=Integer.parseInt(hostname.substring(sep+1,hostname.length()));
				 hostname=hostname.substring(0,sep);
			}			
		}
		if (args.length>=2) {
			try {
				 fSample = Double.parseDouble(args[2]);
			}
			catch (NumberFormatException e) {
				 System.err.println("Error: couldn't understand sample rate. "+fSample+"hz assumed");
			}			 
		}

		if ( args.length>=3 ) {
			try {
				nChannels = Integer.parseInt(args[2]);
			}
			catch (NumberFormatException e) {
				System.err.println("Error: couldn't understand number of channels. "+nChannels+" assumed");
			}			 
		}

		if ( args.length>=4 ) {
			try {
				blockSize = Integer.parseInt(args[3]);
			}
			catch (NumberFormatException e) {
				System.err.println("Error: couldn't understand blockSize. "+blockSize+" assumed");
			}			 
		}		  
		SignalProxy sp=new SignalProxy(hostname,port,nChannels,fSample,blockSize);
		sp.mainloop();
		sp.stop();
	 }

	 SignalProxy(String hostname, int port, int nChannels, double fSample, int blockSize){
		  this.hostname = hostname;
		  this.port     = port;
		  this.nChannels= nChannels;
		  this.fSample  = fSample;
		  this.blockSize= blockSize;
		  client   = new BufferClient();
		  generator= new Random();
	 }

	private double[][] genData() {
		double[][] data = new double[blockSize][nChannels];

		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[x].length-1; y++) {
				data[x][y] = generator.nextDouble();
			}
			// last channel is always pure sin wave
			data[x][data[x].length] = Math.sin( (nSample + x)*sinFreq*2*Math.PI/fSample );
		}

		return data;
	}

	public void mainloop() {
		run = true;
		try {
			if (!client.isConnected()) {
				client.connect(hostname, port);
			} else {
				System.out.println("Could not connect to buffer.");
				return;
			}
			
			System.out.println("Putting header");

			client.putHeader(new Header(nChannels, fSample, 10));
			double[][] data = null;
			long printTime = 0;
			long t0 = System.currentTimeMillis();
			long t  = t0;
			long nextBlockTime = t0;
			while (run) {
				data = genData();
				client.putData(data);
				nSample  = nSample + blockSize; // current total samples sent
				t        = System.currentTimeMillis() - t0; // current time since start
				nextBlockTime = (long)((nSample+blockSize)*1000/fSample); // time to send next block
				if (nextBlockTime > t) {
					 Thread.sleep(nextBlockTime-t);
				}
				if (t > printTime) {
					 System.out.println(  nSample + " samples added.");
					 printTime = printTime + 2000; // 2s between prints
				}				
			}
		} catch (final IOException e) {
			 e.printStackTrace();//android.updateStatus("IOException caught, stopping.");
			return;
		} catch (InterruptedException e) {
			 e.printStackTrace();//android.updateStatus("InterruptedException caught, stopping.");
			return;
		}
	}

	public void stop() {
		 //super.stop();
		try {
			client.disconnect();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}