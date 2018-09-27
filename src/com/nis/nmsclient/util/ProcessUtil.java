package com.nis.nmsclient.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.nis.nmsclient.common.Contants;

public class ProcessUtil
{
	
	static Logger logger = Logger.getLogger(ProcessUtil.class);
	private static final String EXEC_CMD_FILE = Contants.LOCAL_SCRIPT_PATH + "/execCmdBySu.sh";
	private static final String CHECK_USERPASS_FILE = Contants.LOCAL_SCRIPT_PATH
			+ "/check_userpass.sh";
	private static Byte[] oWin = new Byte[0];
	private static Byte[] oLinux = new Byte[0];
	
	/**
	 * 从文件读取PID值
	 * 
	 * @param pidFile
	 *            存放PID的文件
	 * @return PID数组
	 * @throws Exception
	 */
	public static String[] getProcessIdByFile(File pidFile) throws Exception
	{
		String[] pids = null;
		if (pidFile.exists())
		{
			String[] values = FileWrUtil.cfgFileReader(pidFile);
			if (values != null && values.length > 0 && values[0].trim().length() > 0)
			{
				pids = values[0].trim().split(",");
			}
			
		}
		return pids;
	}
	
	/**
	 * 根据PID按不同操作系统判断进程是否存在
	 * 
	 * @param pid
	 * @return true 存在，false 不存在
	 * @throws Exception
	 */
	public static boolean isProcessExistByPid(String pid) throws Exception
	{
		boolean flag = false;
		// 根据操作系统确定获取进程ID的方式
		String os = System.getProperty("os.name");
		if (os.startsWith("Windows"))
		{
			flag = isWinProcessExistByPid(pid.trim());
		} else if (os.startsWith("Linux"))
		{
			flag = isLinuxProcessExistByPid(pid.trim());
		} else
		{
			throw new IOException("unknown operating system: " + os);
		}
		
		return flag;
	}
	
	/**
	 * 根据PID判断Linux下进程是否存在
	 * 
	 * @param pid
	 * @return
	 */
	public static boolean isLinuxProcessExistByPid(String pid)
	{
		String cmd = "ps -p " + pid.trim() + " | grep " + pid.trim()
				+ " | grep -v defunct | awk '{print $1}'";
		String[] shellCmd1 = new String[] { "/bin/bash", "-c", cmd };
		BufferedReader bReader = null;
		BufferedReader errorReader = null;
		Process process = null;
		boolean flag = false;
		logger.info("isLinuxProcessExistByPid cmd-----" + cmd);
		synchronized (oLinux)
		{
			try
			{
				// 找到根据进程名获取到的进程号列表
				process = Runtime.getRuntime().exec(shellCmd1);
				process.getOutputStream().close();
				
				bReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String line = null;
				while ((line = bReader.readLine()) != null)
				{
					logger.debug("isLinuxProcessExistByPid line-----" + line);
					flag = true;
				}
				while ((line = errorReader.readLine()) != null)
				{
					logger.debug("isLinuxProcessExistByPid error line--->" + line);
				}
				
			} catch (IOException e)
			{
				logger.error(Utils.printExceptionStack(e));
			} finally
			{
				try
				{
					if (bReader != null)
					{
						bReader.close();
					}
					if (errorReader != null)
					{
						errorReader.close();
					}
					if (process != null)
					{
						process.destroy();
					}
				} catch (IOException e)
				{
					logger.error(Utils.printExceptionStack(e));
				}
			}
			
		}
		return flag;
	}
	
	/**
	 * 根据PID判断Windows下进程是否存在
	 * 
	 * @param pid
	 * @return
	 */
	public static boolean isWinProcessExistByPid(String pid)
	{
		String[] cmd = new String[] { "cmd.exe", "/C",
				"wmic process where processId=" + pid.trim() + " get processId" };
		logger.info("isWinProcessExistByPid cmd-----" + cmd[2]);
		BufferedReader bReader = null;
		BufferedReader errorReader = null;
		Process process = null;
		boolean flag = false;
		try
		{
			synchronized (oWin)
			{
				process = Runtime.getRuntime().exec(cmd);
				process.getOutputStream().close();
				bReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String line = null;
				while ((line = bReader.readLine()) != null)
				{
					logger.debug("isWinProcessExistByPid line-----" + line);
					if (line.trim().equals(pid.trim()))
					{
						flag = true;
					}
				}
				while ((line = errorReader.readLine()) != null)
				{
					logger.debug("isWinProcessExistByPid error line--->" + line);
				}
			}
			
		} catch (IOException e)
		{
			logger.error(Utils.printExceptionStack(e));
		} finally
		{
			try
			{
				if (bReader != null)
				{
					bReader.close();
				}
				if (errorReader != null)
				{
					errorReader.close();
				}
				if (process != null)
				{
					process.destroy();
				}
			} catch (IOException e)
			{
				logger.error(Utils.printExceptionStack(e));
			}
		}
		
		return flag;
	}
	
	/**
	 * Linux下执行命令
	 * 
	 * @param cmd
	 * @return
	 * @throws Exception
	 */
	public static String execLinuxCmd(String cmd) throws Exception
	{
		BufferedReader bReader = null;
		BufferedReader errorReader = null;
		Process process = null;
		try
		{
			// logger.debug("execLinuxCmd start-------" + cmd);
			String[] shellCmd = new String[] { "/bin/bash", "-c", cmd };
			process = Runtime.getRuntime().exec(shellCmd);
			process.getOutputStream().close();
			
			bReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			String line = null;
			while ((line = bReader.readLine()) != null)
			{
				logger.debug("execLinuxCmd line--->" + line);
			}
			StringBuffer errorSb = new StringBuffer();
			while ((line = errorReader.readLine()) != null)
			{
				logger.debug("execLinuxCmd error line--->" + line);
				errorSb.append(line + ",");
			}
			
			if (errorSb.toString().length() > 0)
			{
				errorSb.deleteCharAt(errorSb.length() - 1);
				return errorSb.toString();
			}
			
			// logger.debug("execLinuxCmd end-------" + cmd);
		} catch (Exception e)
		{
			throw e;
		} finally
		{
			try
			{
				if (bReader != null)
				{
					bReader.close();
				}
				if (errorReader != null)
				{
					errorReader.close();
				}
				if (process != null)
				{
					process.destroy();
				}
			} catch (Exception e1)
			{
			}
		}
		return null;
	}
	
	/**
	 * windows下执行命令
	 * 
	 * @param cmd
	 * @return
	 * @throws Exception
	 */
	public static String execWinCmd(String cmd) throws Exception
	{
		BufferedReader bReader = null;
		BufferedReader errorReader = null;
		Process process = null;
		try
		{
			logger.debug("execWinCmd start-------" + cmd);
			//cmd = handlerWinSpace(cmd);
			String[] cmdArr = new String[] { "cmd.exe", "/C", cmd };
			process = Runtime.getRuntime().exec(cmdArr);
			if (cmd.endsWith(".exe"))
			{
				synchronized (process)
				{
					process.wait(1000 * 5);
				}
				process.getOutputStream().close();
				process.getInputStream().close();
				process.getErrorStream().close();
			} else
			{
				process.getOutputStream().close();
				Charset platform = Charset.forName(System.getProperty("sun.jnu.encoding"));
				bReader = new BufferedReader(new InputStreamReader(process.getInputStream(), platform));
				errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), platform));
				String line = null;
				while ((line = bReader.readLine()) != null)
				{
					logger.debug("execWinCmd line--->" + line);
				}
				StringBuffer errorSb = new StringBuffer();
				while ((line = errorReader.readLine()) != null)
				{
					logger.debug("execWinCmd error--->" + line);
					errorSb.append(line + ",");
				}
				if (errorSb.length() > 0)
				{
					errorSb.deleteCharAt(errorSb.length() - 1);
					return errorSb.toString();
				}
			}
			
			logger.debug("execWinCmd end-------" + cmd);
			
		} catch (Exception e)
		{
			throw e;
		} finally
		{
			try
			{
				if (bReader != null)
				{
					bReader.close();
				}
				if (errorReader != null)
				{
					errorReader.close();
				}
				if (process != null)
				{
					process.destroy();
				}
			} catch (Exception e1)
			{
			}
		}
		
		return null;
	}
	
	/**
	 * 执行文件或命令，不同的操作系统按相应方式执行
	 * 
	 * @param cmd
	 *            文件全路径或执行命令
	 * @param cmdParams
	 *            执行参数
	 * @param username
	 *            执行用户
	 * @param passwd
	 *            执行用户的密码
	 * @return 返回错误输出：若有返回，则执行失败；若返回null，则执行成功
	 * @throws IOException
	 */
	public static String runExec(String cmd, String[] cmdParams, String username, String passwd)
			throws IOException
	{
		return runExec(cmd, cmdParams, username, passwd, false);
	}
	
	/**
	 * 执行文件或命令，不同的操作系统按相应方式执行
	 * 
	 * @param isDaemon
	 *            若为true, NC守护进程, 不读取输出；若为false, 执行文件或命令，读取输出
	 * @return
	 * @throws IOException
	 */
	public static String runExec(String cmd, String[] cmdParams, String username, String passwd,
			boolean isDaemon) throws IOException
	{
		// 根据操作系统确定运行方式
		String os = System.getProperty("os.name");
		Process process = null;
		try
		{
			logger.debug("runExec start-------" + cmd);
			String error = null;
			if (os.startsWith("Windows"))
			{
				StringBuffer sb = new StringBuffer();
				if (isDaemon)
				{// -- 执行NC守护进程脚本
					sb.append("start /b " + handlerWinSpace(cmd));
					if (cmdParams != null && cmdParams.length > 0)
					{
						// 如果执行参数为数组类型时使用
						for (String param: cmdParams)
						{
							// 带空格参数的特殊处理
							sb.append(" " + handlerWinSpace(param));
						}
					}
					sb.append(" >nul 2>&1");
					logger.info("runExec Windows-------" + sb.toString());
					String[] cmdArr = new String[] { "cmd.exe", "/C", sb.toString() };
					process = Runtime.getRuntime().exec(cmdArr);
					synchronized (process)
					{
						process.wait(1000 * 5);
					}
					process.getOutputStream().close();
					process.getInputStream().close();
					process.getErrorStream().close();
				} else
				{
					sb.append(handlerWinSpace(cmd));
					if (cmdParams != null && cmdParams.length > 0)
					{
						// 如果执行参数为数组类型时使用
						for (String param: cmdParams)
						{
							// 带空格参数的特殊处理
							sb.append(" " + handlerWinSpace(param));
						}
					}
					logger.info("runExec Windows-------" + sb.toString());
					error = execWinCmd(sb.toString());
				}
			} else if (os.startsWith("Linux"))
			{
				StringBuffer sb = new StringBuffer();
				sb.append("nohup " + cmd.trim());
				if (cmdParams != null && cmdParams.length > 0)
				{
					// 如果执行参数为数组类型时使用
					for (String param: cmdParams)
					{
						// 带空格参数的特殊处理
						sb.append(" " + handlerLinuxSpace(param));
					}
				}
				sb.append(" >/dev/null 2>&1 &");
				logger.info("runExec Linux-------" + sb.toString());
				if (username != null && username.trim().length() > 0)
				{
					// -- 有密码调用execCmdBySu.sh文件执行
					if (passwd != null && passwd.trim().length() > 0)
					{
						File file = new File(EXEC_CMD_FILE);
						// 执行结果标识参数1、0 表示执行命令后是否添加：echo $?
						String temp = file.getCanonicalPath() + " 1 \"" + sb.toString() + "\" "
								+ username.trim();
						logger.info("runExec-->" + temp);
						temp += " " + DESUtil.desDecrypt(passwd.trim());
						sb.delete(0, sb.length());
						sb.append("nohup " + temp + " >/dev/null 2>&1 &");
					} else
					{
						// -- 无密码自己拼命令
						String temp = "su - -c \"" + sb.toString() + "\" " + username.trim();
						logger.info("runExec-->" + temp);
						sb.delete(0, sb.length());
						sb.append(temp);
					}
				}
				error = execLinuxCmd(sb.toString());
			} else
			{
				throw new IOException("unknown operating system: " + os);
			}
			
			logger.debug("sleep: 10 seconds");
			Thread.sleep(10 * 1000);
			
			logger.debug("runExec end-------" + cmd);
			
			return error;
		} catch (Exception e)
		{
			logger.error(Utils.printExceptionStack(e));
//			return "出现异常：" + e.getMessage();
			return "Abnormality：" + e.getMessage();
		} finally
		{
			try
			{
				if (process != null)
				{
					process.destroy();
				}
			} catch (Exception e1)
			{
			}
		}
		
	}
	
	/**
	 * 赋权限：若path是目录，给目录下所有文件赋指定的权限
	 * 
	 * @param permit
	 *            权限的整数值
	 * @param path
	 *            需要赋权限的文件或目录
	 */
	public static void permit(long permit, File path)
	{
		// 根据操作系统确定运行方式
		String os = System.getProperty("os.name");
		logger.debug("permit-----path=" + path.getAbsolutePath());
		try
		{
			if (os.startsWith("Linux") && path != null && path.exists())
			{
				if (path.isDirectory() && path.listFiles().length > 0)
				{
					execLinuxCmd("chmod " + permit + " "
							+ handlerLinuxSpace(path.getCanonicalPath()) + File.separator + "*");
					File[] files = path.listFiles();
					for (File file: files)
					{
						if (file.isDirectory())
						{
							permit(permit, file);
						}
					}
				} else
				{
					execLinuxCmd("chmod " + permit + " "
							+ handlerLinuxSpace(path.getCanonicalPath()));
				}
			}
		} catch (Exception e)
		{
			logger.error(Utils.printExceptionStack(e));
		}
	}
	
	/**
	 * 杀进程：根据操作系统以不同的方式停用进程
	 * 
	 * @param processId
	 * @throws IOException
	 */
	public static void killProcess(String processId) throws IOException
	{
		// 根据操作系统确定运行方式
		String os = System.getProperty("os.name");
		try
		{
			if (os.startsWith("Windows"))
			{
				String cmd = "wmic process where processId=" + processId + " delete";
				logger.debug("killProcess win cmd-------" + cmd);
				execWinCmd(cmd);
			} else if (os.startsWith("Linux"))
			{
				String cmd = "kill -9 " + processId;
				logger.debug("killProcess linux cmd-------" + cmd);
				execLinuxCmd(cmd);
			} else
			{
				throw new IOException("unknown operating system: " + os);
			}
			Thread.sleep(5 * 1000);
		} catch (Exception e)
		{
			logger.error(Utils.printExceptionStack(e));
		}
	}
	
	/**
	 * 只能用来运行命令，取得运行命令的输出结果，根据错误输出流来判断命令运行成功与否 如果需切换用户，调用execCmdBySu.sh执行
	 * 
	 * @param cmd
	 * @param username
	 * @return 成功：0|命令输出信息；失败：1|失败信息
	 * @throws Exception
	 */
	public static String runExecGetResult(String cmd, String username, String passwd)
			throws Exception
	{
		BufferedReader bReader = null;
		BufferedReader errorReader = null;
		Process process = null;
		String seprator = "|";
		try
		{
			logger.debug("runExecGetResult start-------" + cmd);
			String os = System.getProperty("os.name");// 根据操作系统确定运行方式
			String result = "0" + seprator;
			String[] cmdArr = null;
			if (os.startsWith("Windows"))
			{
				// 【&& echo 0 || echo 1】 执行成功输出0，执行失败输出1，为了与Linux下的一致来取执行结果
				String tempCmd = handlerWinSpace(cmd) + " && echo 0 || echo 1";
				logger.info("runExecGetResult-->" + tempCmd);
				cmdArr = new String[] { "cmd.exe", "/C", tempCmd };
			} else if (os.startsWith("Linux"))
			{
				String tempCmd = cmd.trim();
				if (username != null && username.trim().length() > 0)
				{
					// -- 有密码调用execCmdBySu.sh文件执行
					if (passwd != null && passwd.trim().length() > 0)
					{
						File file = new File(EXEC_CMD_FILE);
						// 执行结果标识参数1、0 表示执行命令后是否添加：echo $?
						tempCmd = file.getCanonicalPath() + " 0 \"" + cmd.trim() + "\" "
								+ username.trim();
						logger.info("runExecGetResult-->" + tempCmd);
						tempCmd += " " + DESUtil.desDecrypt(passwd.trim());
					} else
					{
						// -- 无密码自己拼命令
						tempCmd = "su - -c \"" + cmd.trim() + ";echo $?\" " + username.trim();
						logger.info("runExecGetResult-->" + tempCmd);
					}
				} else
				{
					tempCmd += ";echo $?";// 为了与脚本中的echo $?运行命令保持一致
					logger.info("runExecGetResult-->" + tempCmd);
				}
				cmdArr = new String[] { "/bin/bash", "-c", tempCmd };
			} else
			{
				throw new IOException("unknown operating system: " + os);
			}
			process = Runtime.getRuntime().exec(cmdArr);
			process.getOutputStream().close();
			bReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			String line = null;
			StringBuffer sb = new StringBuffer();
			while ((line = bReader.readLine()) != null)
			{
				sb.append(line + ",");
				logger.debug("runExecGetResult right-->" + line);
			}
			StringBuffer errorSb = new StringBuffer();
			while ((line = errorReader.readLine()) != null)
			{
				errorSb.append(line + ",");
				logger.debug("runExecGetResult error-->" + line);
			}
			if (errorSb.length() > 0)
			{
				errorSb.deleteCharAt(errorSb.length() - 1);
				result = "1" + seprator + errorSb.toString();
			} else if (sb.length() > 0)
			{
				sb.deleteCharAt(sb.length() - 1);// 去掉最后一个逗号
				int lastIndex = sb.lastIndexOf(",");
				String flag = "";
				logger.debug("runExecGetResult lastIndex-->" + lastIndex);
				if (lastIndex != -1)
				{
					// 配合运行命令（echo $?），最后一行为结果标识
					flag = sb.substring(lastIndex + 1, sb.length()).trim();
					sb.delete(lastIndex, sb.length());// 删除结果标识和其前面的逗号
				} else
				{
					flag = sb.toString().trim();
					sb.delete(0, sb.length());
				}
				logger.debug("runExecGetResult flag-->" + flag);
				// 配合运行命令（echo $?）：最后一行若是0，则执行成功，若非0，则失败
				if ("0".equals(flag))
				{
					result = "0" + seprator + sb.toString();
				} else
				{
					result = "1" + seprator + sb.toString();
				}
			}
			logger.debug("runExecGetResult result-->" + result);
			logger.debug("runExecGetResult end-------" + cmd);
			
			return result;
		} catch (Exception e)
		{
			throw e;
		} finally
		{
			try
			{
				if (bReader != null)
				{
					bReader.close();
				}
				if (errorReader != null)
				{
					errorReader.close();
				}
				if (process != null)
				{
					process.destroy();
				}
			} catch (Exception e1)
			{
			}
		}
	}
	
	/**
	 * 验证用户名或用户组是否存在
	 * 
	 * @param username
	 * @param userGroup
	 * @return
	 * @throws Exception
	 */
	public static boolean checkUserOrGroupExist(String username, String userGroup) throws Exception
	{
		try
		{
			boolean flag = false;
			logger.debug("checkUserOrGroupExist start-------");
			String os = System.getProperty("os.name");// 根据操作系统确定运行方式
			if (os.startsWith("Windows"))
			{
				flag = true;
			} else if (os.startsWith("Linux"))
			{
				if (username == null && userGroup == null)
				{
					flag = true;
				} else
				{
					StringBuffer sb = new StringBuffer();
					if (username != null)
					{
						sb.append("su " + username.trim() + ";");
					}
					if (userGroup != null)
					{
						sb.append("groups " + userGroup.trim() + ";");
					}
					if (execLinuxCmd(sb.toString()) == null)
					{// 没有返回值，验证通过
						flag = true;
					}
				}
			} else
			{
				throw new IOException("unknown operating system: " + os);
			}
			
			logger.debug("checkUserOrGroupExist end-------");
			
			return flag;
		} catch (Exception e)
		{
			throw e;
		}
		
	}
	
	/**
	 * 验证用户名和密码是否正确，调用check_userpass.sh脚本
	 * 
	 * @param username
	 * @param passwd
	 */
	public static boolean checkUserPass(String username, String passwd) throws Exception
	{
		BufferedReader bReader = null;
		BufferedReader errorReader = null;
		Process process = null;
		try
		{
			boolean flag = false;
			logger.debug("checkUserPass start-------");
			String os = System.getProperty("os.name");// 根据操作系统确定运行方式
			if (os.startsWith("Windows"))
			{
				flag = true;
			} else if (os.startsWith("Linux"))
			{
				File file = new File(CHECK_USERPASS_FILE);
				StringBuffer sb = new StringBuffer();
				sb.append(file.getCanonicalPath() + " " + Utils.getLocalIp());
				if (username != null)
				{
					sb.append(" " + username.trim());
					if (passwd != null)
					{
						sb.append(" " + DESUtil.desDecrypt(passwd.trim()));
					}
				}
				sb.append(" >/dev/null 2>&1;");
				sb.append("echo $?");
				
				String[] cmdArr = new String[] { "/bin/bash", "-c", sb.toString() };
				process = Runtime.getRuntime().exec(cmdArr);
				process.getOutputStream().close();
				bReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String line = null;
				while ((line = bReader.readLine()) != null)
				{
					logger.debug("checkUserPass line-------" + line);
					if (line.trim().equals("0"))
					{
						flag = true;
					}
				}
				while ((line = errorReader.readLine()) != null)
				{
					logger.debug("checkUserPass error line--->" + line);
				}
			} else
			{
				throw new IOException("unknown operating system: " + os);
			}
			
			logger.debug("runExecGetResult end-------");
			
			return flag;
		} catch (Exception e)
		{
			throw e;
		} finally
		{
			try
			{
				if (bReader != null)
				{
					bReader.close();
				}
				if (errorReader != null)
				{
					errorReader.close();
				}
				if (process != null)
				{
					process.destroy();
				}
			} catch (Exception e1)
			{
			}
		}
	}
	
	/**
	 * 检查文件或命令是否存在且可执行
	 * 
	 * @param cmd
	 * @return true 是，false 否
	 * @throws IOException
	 */
	public static boolean isFileOrCmdExist(String cmd) throws IOException
	{
		BufferedReader bReader = null;
		BufferedReader errorReader = null;
		Process process = null;
		boolean flag = false;
		logger.debug("isFileOrCmdExist start-------" + cmd);
		try
		{
			// 根据操作系统确定运行方式
			String os = System.getProperty("os.name");
			if (os.startsWith("Windows"))
			{
				logger.debug("isFileOrCmdExist Windows-------" + cmd);
				// 待实现: 目前只处理了文件，执行命令未实现判断
				File startFile = new File(cmd);
				if (startFile.exists() && startFile.canExecute())
				{
					flag = true;
				}
			} else if (os.startsWith("Linux"))
			{
				String cmd1 = "[ -x " + cmd + " ] || which " + cmd + ";echo $?";
				logger.debug("isFileOrCmdExist linux-------" + cmd1);
				String[] shellCmd = new String[] { "/bin/bash", "-c", cmd1 };
				process = Runtime.getRuntime().exec(shellCmd);
				process.getOutputStream().close();
				
				bReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String line = null;
				while ((line = bReader.readLine()) != null)
				{
					logger.debug("isFileOrCmdExist linux--->" + line);
					if (line.trim().equals("0"))
					{
						flag = true;
					}
				}
				while ((line = errorReader.readLine()) != null)
				{
					logger.debug("isFileOrCmdExist linux error--->" + line);
				}
			} else
			{
				throw new IOException("unknown operating system: " + os);
			}
			
		} catch (Exception e)
		{
			logger.error(Utils.printExceptionStack(e));
			return false;
		} finally
		{
			try
			{
				if (bReader != null)
				{
					bReader.close();
				}
				if (errorReader != null)
				{
					errorReader.close();
				}
				if (process != null)
				{
					process.destroy();
				}
			} catch (Exception e1)
			{
			}
		}
		logger.debug("isFileOrCmdExist end-------" + cmd);
		
		return flag;
	}
	
	/**
	 * 
	 * TODO 处理路径中带有空格 括号的问题 在外部加入""
	 * 
	 * @author jinshujuan May 30, 2013
	 * @version 1.0
	 * @param str
	 * @return
	 */
	public static String handlerWinPath(String str)
	{
		
		if (str != null && !str.equals(""))
		{
			if (str.indexOf(" ") != -1 || str.indexOf("(") != -1 || str.indexOf(")") != -1)
			{
				str = "\"" + str + "\"";
			}
		}
		logger.debug("handlerWinPath---str=" + str);
		return str;
	}
	
	/**
	 * 对Windows下路径中带空格的特殊处理
	 * 
	 * @param str
	 * @return
	 */
	public static String handlerWinSpace(String str)
	{
		/*
		 * 注意：：若最后一级路径出现空格，处理上的存在Bug -- 需要在最后一级也加上\\或/ 如：xcopy /y D:\\Program
		 * Files\\Test 1\\ D:\\Program Files\\Test 2\\ /s/e 可以正常替换空格 xcopy /y
		 * D:\\Program Files\\Test 1 D:\\Program Files\\Test 2 /s/e
		 * 不能正常替换空格，最后一级Test 1和Test 2中的空格替换不了。
		 */
		if (str != null)
		{
			if (str.indexOf(":\\") != -1)
			{
				// 针对这样的命令, 如:
				// D:\Program Files\Test\test.bat >> D:\Program Files\test.log;
				// xcopy /y D:\Program Files\Test D:\Program Files\Test2 /s/e
				// 先取第一个:\之后，每次再取与\之间的串循环替换空格
				// 若与\之间的串中有:\（/test.bat >> D:/, /Test D:/）则不用替换空格
				int index = str.indexOf(":\\");
				String start = str.substring(0, index + 2);
				String end = str.substring(index + 2, str.length());
				while (end.indexOf("\\") != -1)
				{
					index = end.indexOf("\\");
					String middle = end.substring(0, index + 1);
					if (middle.endsWith(":\\"))
					{
						start += middle;
					} else
					{
						start += middle.replace(" ", "\" \"");
					}
					end = end.substring(index + 1, end.length());
				}
				start += end;
				str = start;
			} else if (str.indexOf(":/") != -1)
			{
				// 针对这样的命令, 如:
				// D:/Program Files/Test/test.bat >> D:/Program Files/test.log;
				// xcopy /y D:/Program Files/Test D:/Program Files/Test2 /s/e
				// 先取第一个:/之后，每次再取与/之间的串循环替换空格
				// 若与/之间的串中有:/（/test.bat >> D:/, /Test D:/）或 /（/Test2 /）则不用替换空格
				int index = str.indexOf(":/");
				String start = str.substring(0, index + 2);
				String end = str.substring(index + 2, str.length());
				while (end.indexOf("/") != -1)
				{
					index = end.indexOf("/");
					String middle = end.substring(0, index + 1);
					if (middle.endsWith(":/") || middle.endsWith(" /"))
					{
						start += middle;
					} else
					{
						start += middle.replace(" ", "\" \"");
					}
					end = end.substring(index + 1, end.length());
				}
				start += end;
				str = start;
			}
		}
		logger.debug("handlerWinSpace---str=" + str);
		return str;
	}
	
	/**
	 * 对Linux下路径中带空格的特殊处理
	 * 
	 * @param str
	 * @return
	 */
	public static String handlerLinuxSpace(String str)
	{
		if (str != null)
		{
			str = str.trim().replace(" ", "\\ ");
		}
		return str;
	}
	
	/**
	 * 根据关键字查找进程PID
	 * 
	 * @param procSearchKey
	 * @return pid列表
	 * @throws Exception
	 */
	public static List<String> getProcessIdBySearchKey(String procSearchKey) throws Exception
	{
		List<String> value = null;
		// 根据操作系统确定获取进程ID的方式
		String os = System.getProperty("os.name");
		if (os.startsWith("Windows"))
		{
			value = getWindowsProcessId(procSearchKey);
		} else if (os.startsWith("Linux"))
		{
			value = getLinuxProcessId(procSearchKey);
		} else
		{
			throw new IOException("unknown operating system: " + os);
		}
		
		return value;
	}
	
	/**
	 * Windows下根据关键字查找进程，关键字不区分大小写
	 * 
	 * @param procSearchKey
	 * @return
	 */
	public static List<String> getWindowsProcessId(String procSearchKey)
	{
		// "wmic process get
		// Caption,commandLine,Description,ExecutablePath,Name,processId|findstr
		// /i " + procSearchKey + "|findstr /v findstr"
		String[] cmd = new String[] {
				"cmd.exe",
				"/C",
				"wmic process where \"Caption like '%%" + procSearchKey
						+ "%%' or CommandLine like '%%" + procSearchKey
						+ "%%' or Description like '%%" + procSearchKey
						+ "%%' or ExecutablePath like '%%" + procSearchKey + "%%' or Name like '%%"
						+ procSearchKey
						+ "%%'\" get Name,ProcessId,CommandLine|findstr /v wmic|findstr /v findstr" };
		logger.info("getWindowsProcessId-----" + cmd[2]);
		BufferedReader bReader = null;
		BufferedReader errorReader = null;
		Process process = null;
		List<String> processIDList = null;
		try
		{
			synchronized (oWin)
			{
				process = Runtime.getRuntime().exec(cmd);
				process.getOutputStream().close();
				bReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String line = null;
				processIDList = new ArrayList<String>();
				while ((line = bReader.readLine()) != null)
				{
					logger.debug("getWindowsProcessId ---- line >> " + line);
					String[] tmp = line.split("\\s{2,}");
					if (tmp == null)
					{
						continue;
					}
					for (int i = 0; i < tmp.length; i++)
					{
						if (tmp[i].matches("\\d+"))
						{
							processIDList.add(tmp[i]);
						}
					}
				}
				while ((line = errorReader.readLine()) != null)
				{
					logger.debug("getWindowsProcessId error line--->" + line);
				}
			}
			
		} catch (IOException e)
		{
			logger.error(Utils.printExceptionStack(e));
			return null;
		} finally
		{
			try
			{
				if (bReader != null)
				{
					bReader.close();
				}
				if (errorReader != null)
				{
					errorReader.close();
				}
				if (process != null)
				{
					process.destroy();
				}
			} catch (IOException e)
			{
				logger.error(Utils.printExceptionStack(e));
			}
		}
		
		return processIDList;
	}
	
	/**
	 * Linux下根据关键字查找进程，关键字不区分大小写
	 * 
	 * @param procSearchKey
	 * @return
	 */
	public static List<String> getLinuxProcessId(String procSearchKey)
	{
		String cmd = "ps -ef | grep -i '" + procSearchKey.trim()
				+ "' | grep -v grep | awk '{print $2}'";
		logger.info("getLinuxProcessId-----" + cmd);
		String[] shellCmd1 = new String[] { "/bin/bash", "-c", cmd };
		BufferedReader bReader = null;
		BufferedReader errorReader = null;
		Process process = null;
		List<String> processIDList = null;
		synchronized (oLinux)
		{
			try
			{
				// 找到根据进程名获取到的进程号列表
				process = Runtime.getRuntime().exec(shellCmd1);
				process.getOutputStream().close();
				
				bReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String line = null;
				processIDList = new ArrayList<String>();
				while ((line = bReader.readLine()) != null)
				{
					processIDList.add(line);
					logger.debug("getLinuxProcessId ---- line >> " + line);
				}
				while ((line = errorReader.readLine()) != null)
				{
					logger.debug("getLinuxProcessId error line--->" + line);
				}
			} catch (IOException e)
			{
				logger.error(Utils.printExceptionStack(e));
			} finally
			{
				try
				{
					if (bReader != null)
					{
						bReader.close();
					}
					if (errorReader != null)
					{
						errorReader.close();
					}
					if (process != null)
					{
						process.destroy();
					}
				} catch (IOException e)
				{
					logger.error(Utils.printExceptionStack(e));
				}
			}
			
		}
		return processIDList;
	}
	
	/**
	 * 检查PID是否存在，存在返回PID，不存在返回描述信息
	 * 检查逻辑：先检查PID文件，取出PID，验证PID指定进程是否存在，不存在再使用搜索关键字查找进程 isExistFlag: 0 进程不存在，1
	 * 进程存在且仅找到一个，2 进程存在且找到多个
	 * 
	 * @return 数组：{进程是否存在的标识(0|1|2), 进程PID或者描述信息}
	 */
	public static Object[] checkPidAndGetPid(String pidFile, String procSearchKey) throws Exception
	{
		int isExistFlag = 0;
		String pidInfo = "";
		if (pidFile != null && !"".equals(pidFile))
		{
			File file = new File(pidFile);
			if (file.exists())
			{
				String[] pidArr = ProcessUtil.getProcessIdByFile(file);
				if (pidArr != null && pidArr.length > 0)
				{// 目前考虑只有一个PID的情况
					boolean isExist = ProcessUtil.isProcessExistByPid(pidArr[0]);
					if (isExist)
					{
						pidInfo = pidArr[0];
						isExistFlag = 1;
					} else
					{
//						pidInfo = "进程PID\"" + pidArr[0] + "\"不存在";
						pidInfo = "i18n_client.ProcessUtil.processPid_n81i\"" + pidArr[0] + "\"i18n_client.ProcessUtil.notExists_n81i";
					}
				}
			} else
			{
//				pidInfo = "PID文件\"" + file.getAbsolutePath() + "\"不存在";
				pidInfo = "i18n_client.ProcessUtil.pidFile_n81i\"" + file.getAbsolutePath() + "\"i18n_client.ProcessUtil.notExists_n81i";
			}
		} else
		{
//			pidInfo = "PID文件字段为空";
			pidInfo = "i18n_client.ProcessUtil.pidFieldNull_n81i";
		}
		
		if (isExistFlag == 0 && procSearchKey != null && !"".equals(procSearchKey))
		{
			// PID文件或PID文件中的PID不存在，则使用进程搜索关键字查找PID
			List<String> pidList = ProcessUtil.getProcessIdBySearchKey(procSearchKey);
			if (pidList == null || pidList.size() == 0)
			{
//				pidInfo += (pidInfo.length() > 0 ? ", " : "") + "进程搜索关键字" + procSearchKey + "未找到进程";
				pidInfo += (pidInfo.length() > 0 ? ", " : "") + "i18n_client.ProcessUtil.searchKey_n81i" + procSearchKey + "i18n_client.ProcessUtil.noProcess_n81i";
			} else if (pidList.size() > 1)
			{
				pidInfo += (pidInfo.length() > 0 ? ", " : "") + "i18n_client.ProcessUtil.searchKey_n81i" + procSearchKey
						+ " i18n_client.ProcessUtil.findTooMuch_n81i";
				isExistFlag = 2;
			} else
			{// 只找到一个进程
				pidInfo = pidList.get(0);
				isExistFlag = 1;
			}
		}
		
		return new Object[] { isExistFlag, pidInfo };
	}
	
	/*
	 * public static String getProcessId(String processName, String processPath)
	 * throws Exception{ String value = null; //根据操作系统确定获取进程ID的方式 String os =
	 * System.getProperty("os.name"); if (os.startsWith("Windows")) { value =
	 * getWindowsProcessId(processName, processPath); }else if
	 * (os.startsWith("Linux")){ value = getLinuxProcessId(processName,
	 * processPath); } else { throw new IOException("unknown operating system: " +
	 * os); }
	 * 
	 * return value; }
	 * 
	 * public static String getWindowsProcessId(String procName, String
	 * procPath) { String[] cmd = new String[] { "cmd.exe", "/C", "wmic process
	 * where \"name='" + procName.trim() + "' and executablepath='" +
	 * procPath.trim().replace("\\", "\\\\") + "'\" get processid" };// list
	 * full logger.debug("cmd-----"+cmd[2]); BufferedReader bReader = null;
	 * Process process = null; String processId = null; try { synchronized
	 * (oWin) { process = Runtime.getRuntime().exec(cmd);
	 * process.getOutputStream().close(); bReader = new BufferedReader(new
	 * InputStreamReader(process .getInputStream())); String line = null; while
	 * ((line = bReader.readLine()) != null) { logger.debug("line-----" + line);
	 * if (line.trim().matches("\\d+")) { processId = line.trim(); } } } } catch
	 * (IOException e) { logger.error(Utils.printExceptionStack(e)); return
	 * processId; }finally{ try { if (bReader != null) { bReader.close(); } if
	 * (process != null) { process.destroy(); } } catch (IOException e) {
	 * logger.error(Utils.printExceptionStack(e)); } }
	 * 
	 * return processId; }
	 * 
	 * public static String getLinuxProcessId(String procName, String procPath) {
	 * String cmd = "ps -ef | grep '" + procName.trim() + "' | grep -v grep |
	 * awk '{print $2}'"; String[] shellCmd1 = new String[] { "/bin/bash", "-c",
	 * cmd }; String[] shellCmd2 = new String[] { "/bin/bash", "-c", cmd };
	 * BufferedReader bReader = null; List<String> processIDList = null;
	 * Process process = null; synchronized (oLinux) { try { // 找到根据进程名获取到的进程号列表
	 * process = Runtime.getRuntime().exec(shellCmd1);
	 * process.getOutputStream().close();
	 * 
	 * bReader = new BufferedReader(new InputStreamReader(process
	 * .getInputStream())); String line = null; processIDList = new ArrayList<String>();
	 * while ((line = bReader.readLine()) != null) { processIDList.add(line); } }
	 * catch (IOException e) { logger.error(Utils.printExceptionStack(e)); }
	 * finally { try { if (bReader != null) { bReader.close(); } if (process !=
	 * null) { process.destroy(); } } catch (IOException e) {
	 * logger.error(Utils.printExceptionStack(e)); } }
	 * 
	 * for (int i = 0; i < processIDList.size(); i++) { shellCmd2 = new String[] {
	 * "/bin/bash", "-c", "lsof -p " + processIDList.get(i) + " | grep 'txt'
	 * |grep -v grep | awk '{print $NF}'" }; try { process =
	 * Runtime.getRuntime().exec(shellCmd2); process.getOutputStream().close();
	 * bReader = new BufferedReader(new InputStreamReader(process
	 * .getInputStream())); String line = null; while ((line =
	 * bReader.readLine()) != null) { if (line.trim().equals(procPath.trim()))
	 * return processIDList.get(i); } } catch (IOException e) {
	 * logger.error(Utils.printExceptionStack(e)); } finally { try { if (bReader !=
	 * null) { bReader.close(); } if (process != null) { process.destroy(); } }
	 * catch (IOException e) { logger.error(Utils.printExceptionStack(e)); } }
	 * }//for end } return null; }
	 */

}
