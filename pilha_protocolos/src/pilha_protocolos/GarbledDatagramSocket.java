package pilha_protocolos;

import java.net.*;
import java.util.Random;

public class GarbledDatagramSocket extends DatagramSocket
{
    // The variables to store the parameters. The values should be
    // between 0 and 100, and their sum should be < 100. All default
    // to 0.
    private int loss_frac = 0;
    private int corrupt_frac = 0;
    private int dup_frac = 0;
    private Random rand_gen;

    public GarbledDatagramSocket() 
	throws SocketException, java.io.IOException
    {
	super();
	rand_gen = new Random();
    }
    
    public GarbledDatagramSocket(int lf, int cf, int df) 
	throws SocketException, java.io.IOException
    {
	super();
	rand_gen = new Random();
	setArgs(lf, cf, df);
    }
    
    public GarbledDatagramSocket(int port, int lf, int cf, int df) 
	throws SocketException, java.io.IOException
    {
	super(port);
	rand_gen = new Random();
	setArgs(lf, cf, df);
    }

    public GarbledDatagramSocket(InetAddress laddr, int port, int lf, int cf, int df) 
	throws SocketException, java.io.IOException
    {
	super(port, laddr);
	rand_gen = new Random();
	setArgs(lf, cf, df);
    }

    public GarbledDatagramSocket(SocketAddress bindaddr, int port, int lf, int cf, int df) 
	throws SocketException, java.io.IOException
    {
	super(bindaddr);
	rand_gen = new Random();
	setArgs(lf, cf, df);
    }

    public void setArgs(int lf, int cf, int df) throws java.io.IOException
    {
	if (lf < 0 || lf > 100)
	    {
		throw new IllegalArgumentException("Illegal Loss Fraction value");
	    }
	
	if (cf < 0 || cf > 100)
	    {
		throw new IllegalArgumentException("Illegal Corruption Fraction value");
	    }
	
	if (df < 0 || df > 100)
	    {
		throw new IllegalArgumentException("Illegal Duplication Fraction value");
	    }
	
	if (lf + cf + df > 100)
	    {
		throw new IllegalArgumentException("The sum of the fractions cannot exceed 1!");
	    }
	
	loss_frac = lf;
	corrupt_frac = cf;
	dup_frac = df;

// 	System.out.println("lf, cf, df: "+ lf + " " + cf + " " + df);
	
    }

    public void send(DatagramPacket p) throws java.io.IOException
    {
	// Get the next random value from the pseudo-rand generator.
	int rand_val = rand_gen.nextInt(101);
	
// 	System.out.println("loss_frac, corrupt_frac, dup_frac: "+ loss_frac + " " + corrupt_frac + " " + dup_frac + "rand_val :" +rand_val);
	if (rand_val < loss_frac)
	    {
		// packet lost, do nothing...
// 		System.out.println("Packet lost");
		
		return;
	    }

	if (rand_val < loss_frac + corrupt_frac)
	    {
		// packet is corrupted, select a byte and scramble it

                System.out.printf("aqui\n\n\n");
		int corrupt_ind = rand_gen.nextInt(p.getLength());
// 		System.out.println("Add port: "+p.getAddress() + p.getPort());
		
		byte[] data_buffer = p.getData();
		byte[] temp_buffer = new byte[1];
		rand_gen.nextBytes(temp_buffer);
		
		data_buffer[corrupt_ind] = temp_buffer[0];
		
		p.setData(data_buffer);
// 		System.out.println("After corruption, Add port: "+p.getAddress()+" " + p.getPort());
		super.send(p);

		return;
	    }

	if (rand_val < loss_frac + corrupt_frac + dup_frac)
	    {
		// packet is to be duplicated...so we call send twice.
// 		System.out.println("pkt dup ed");
		System.out.printf("duplo\n");
		super.send(p);
		super.send(p);

		return;
	    }

	super.send(p);
// 	System.out.println("real send");
	
	return;
    }   
}
