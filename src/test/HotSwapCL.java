package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import org.apache.log4j.Logger;

import com.nis.nmsclient.util.Utils;

class HotSwapCL extends ClassLoader {
	private Logger logger = Logger.getLogger(ClassLoader.class);
	private String basedir; // 需要该类加载器直接加载的类文件的基目录
	private HashSet dynaclazns; // 需要由该类加载器直接加载的类名

	public HotSwapCL(String basedir, String[] clazns) {
		super(null); // 指定父类加载器为 null
		this.basedir = basedir;
		dynaclazns = new HashSet();
		loadClassByMe(clazns);
	}

	private void loadClassByMe(String[] clazns) {
		for (int i = 0; i < clazns.length; i++) {
			try {
				loadDirectly(clazns[i]);
			} catch (Exception e) {
				logger.error(Utils.printExceptionStack(e));
			}
			dynaclazns.add(clazns[i]);
		}
	}

	private Class loadDirectly(String name) throws Exception{
		Class cls = null;
		StringBuffer sb = new StringBuffer(basedir);
		String classname = name.replace('.', File.separatorChar) + ".class";
		sb.append(File.separator + classname);

		File classF = new File(sb.toString());
		cls = instantiateClass(name, new FileInputStream(classF), classF
				.length());
		return cls;
	}

	private Class instantiateClass(String name, InputStream fin, long len) throws Exception{
		byte[] raw = new byte[(int) len];
		try {
			fin.read(raw);
		} catch (IOException e) {
			throw e;
		}finally{
			if(fin!=null){
				fin.close();
			}
		}
		return defineClass(name, raw, 0, raw.length);
	}

	protected Class loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		Class cls = null;
		cls = findLoadedClass(name);
		if (!this.dynaclazns.contains(name) && cls == null)
			cls = getSystemClassLoader().loadClass(name);
		if (cls == null)
			throw new ClassNotFoundException(name);
		if (resolve)
			resolveClass(cls);
		return cls;
	}
}
