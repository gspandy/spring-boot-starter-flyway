package org.flywaydb.spring.boot.ext;

import java.io.File;
import java.lang.reflect.Field;

import org.apache.commons.io.FilenameUtils;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.internal.util.Location;
import org.flywaydb.core.internal.util.Locations;
import org.flywaydb.core.internal.util.scanner.Resource;
import org.flywaydb.core.internal.util.scanner.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

/**
 * 
 * @className	： FlywayMigration
 * @description	： TODO(描述这个类的作用)
 * @author 		： <a href="https://github.com/vindell">vindell</a>
 * @date		： 2017年8月20日 下午5:22:02
 * @version 	V1.0
 */
public class FlywayMigration implements ObjectProvider<FlywayMigrationStrategy>, FlywayMigrationStrategy {
	
	protected static Logger LOG = LoggerFactory.getLogger(FlywayMigration.class);
	/**
     * Whether SQL should be migrated. (default: true)
     */
    private boolean ignoreMigration = true;
    /**
     * Whether SQL should be delete after migrated. (default: true)
     */
    private boolean clearMigrated = true;
    /**
     * Whether SQL should be Rename after migrated. (default: true)
     */
    private boolean renameMigrated = false;
    /**
     * The file name suffix for sql migrations after migrated. (default: .back)
     */
    private String sqlRenameSuffix = ".back";
    
	
	protected Object getField(String fieldName,Object target) {
		Field field = null;
		for (Class<?> superClass = target.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()) {
			try {
				field = superClass.getDeclaredField(fieldName);
			} catch (NoSuchFieldException e) {
			}
		}
		if (field == null) {
			throw new IllegalArgumentException("Could not find field [" + fieldName + "] on target [" + target + "]");
		}
		Object result = null;
		try {
			if (field.isAccessible()) {
				result = field.get(target);
			} else {
				field.setAccessible(true);
				result = field.get(target);
				field.setAccessible(false);
			}
		} catch (IllegalAccessException e) {
			LOG.error(e.getMessage());
		}
		return result;
	}
	
	protected void clearMigrated(Flyway flyway, Scanner scanner ,String prefix, String separator, String suffix) {
		//利用反射取出路径对象信息
		Locations locations = (Locations) getField("locations", flyway);
		//循环路径信息
		for (Location location : locations.getLocations()) {
			//扫描SQL文件
			for (Resource resource : scanner.scanForResources(location, prefix , suffix )) {
				// 物理路径
				String locationOnDisk =  resource.getLocationOnDisk();
	            try {
					// 文件对象
					File fileOnDisk = new File(locationOnDisk);
					// 移除文件
					fileOnDisk.delete();
					
					LOG.info("Delete Migrated SQL File [" + locationOnDisk + "] Success .");
					
				} catch (Exception e) {
					LOG.error("Delete Migrated SQL File [" + locationOnDisk + "] Failed . ", e);
				}
	        }
			
        }
	}
	
	protected void renameMigrated(Flyway flyway, Scanner scanner ,String prefix, String separator, String suffix) {
		//利用反射取出路径对象信息
		Locations locations = (Locations) getField("locations", flyway);
		//循环路径信息
		for (Location location : locations.getLocations()) {
			//扫描SQL文件
			for (Resource resource : scanner.scanForResources(location, prefix , suffix )) {
				// 物理路径
				String locationOnDisk =  resource.getLocationOnDisk();
	            try {
					// 文件对象
					File fileOnDisk = new File(locationOnDisk);
					// 重命名文件
					File dest = new File(fileOnDisk.getParentFile(),FilenameUtils.getBaseName(locationOnDisk) + this.getSqlRenameSuffix());
					fileOnDisk.renameTo(dest);
					
					LOG.info("Rename Migrated SQL File [" + locationOnDisk + "] to [" + dest.getAbsolutePath() + "] Success .");
					
				} catch (Exception e) {
					LOG.error("Rename Migrated SQL File [" + locationOnDisk + "] Failed . ", e);
				}
	        }
        }
	}
	
	/**
	 * @return the ignoreMigration
	 */
	public boolean isIgnoreMigration() {
		return ignoreMigration;
	}

	/**
	 * @param ignoreMigration the ignoreMigration to set
	 */
	public void setIgnoreMigration(boolean ignoreMigration) {
		this.ignoreMigration = ignoreMigration;
	}

	/**
	 * @return the clearMigrated
	 */
	public boolean isClearMigrated() {
		return clearMigrated;
	}

	/**
	 * @param clearMigrated the clearMigrated to set
	 */
	public void setClearMigrated(boolean clearMigrated) {
		this.clearMigrated = clearMigrated;
	}

	/**
	 * @return the renameMigrated
	 */
	public boolean isRenameMigrated() {
		return renameMigrated;
	}

	/**
	 * @param renameMigrated the renameMigrated to set
	 */
	public void setRenameMigrated(boolean renameMigrated) {
		this.renameMigrated = renameMigrated;
	}

	/**
	 * @return the sqlRenameSuffix
	 */
	public String getSqlRenameSuffix() {
		return sqlRenameSuffix;
	}

	/**
	 * @param sqlRenameSuffix the sqlRenameSuffix to set
	 */
	public void setSqlRenameSuffix(String sqlRenameSuffix) {
		this.sqlRenameSuffix = sqlRenameSuffix;
	}

	@Override
	public FlywayMigrationStrategy getObject() throws BeansException {
		return this;
	}

	@Override
	public FlywayMigrationStrategy getObject(Object... args) throws BeansException {
		return this;
	}

	@Override
	public FlywayMigrationStrategy getIfAvailable() throws BeansException {
		return this;
	}

	@Override
	public FlywayMigrationStrategy getIfUnique() throws BeansException {
		return this;
	}

	@Override
	public void migrate(Flyway flyway) {
		//是否忽略升级
		if(this.isIgnoreMigration()){
			
			LOG.info("Flyway Migration has ignored . ");
	        
		} else {

			LOG.info("[Start] Flyway Migration run .. ");
			
	        try {
	        	
	        	// 执行migrate操作
				flyway.migrate();
				
				//清除已升级过的SQL文件
				if(this.isClearMigrated()){
				
					//SQL扫描器
					Scanner scanner = new Scanner(flyway.getClassLoader());
					
					//执行完成SQL版本升级后，删除已升级的脚步，防止有人改动数据库表中的版本号，导致SQL再次执行
					this.clearMigrated(flyway, scanner , flyway.getSqlMigrationPrefix(),flyway.getSqlMigrationSeparator(),flyway.getSqlMigrationSuffix());
					this.clearMigrated(flyway, scanner , flyway.getRepeatableSqlMigrationPrefix(),flyway.getSqlMigrationSeparator(),flyway.getSqlMigrationSuffix());
				
				}
				//重命名已升级过的文件
				else if(this.isRenameMigrated()){
					
					//SQL扫描器
					Scanner scanner = new Scanner(flyway.getClassLoader());
					
					//执行完成SQL版本升级后，重命名已升级的脚步，防止有人改动数据库表中的版本号，导致SQL再次执行
					this.renameMigrated(flyway, scanner , flyway.getSqlMigrationPrefix(),flyway.getSqlMigrationSeparator(),flyway.getSqlMigrationSuffix());
					this.renameMigrated(flyway, scanner , flyway.getRepeatableSqlMigrationPrefix(),flyway.getSqlMigrationSeparator(),flyway.getSqlMigrationSuffix());
					
				}
				
			} catch (Exception e) {
				
				LOG.error("Flyway Migrated Error . ", e);
				
			}
	        LOG.info("[End] Flyway Migration run .. ");
			
		}
	}
	
	
	
}
