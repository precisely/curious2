package us.wearecurio.model;

import org.apache.commons.logging.LogFactory

import grails.converters.*

import us.wearecurio.utility.Utils

class PlotData {
	
	private static def log = LogFactory.getLog(this)

	Long userId
	String name
	Date created
	Date modified
	String jsonPlotData
	Boolean isSnapshot

	public static final int MAXPLOTDATALENGTH = 500000

	static constraints = {
		jsonPlotData(maxSize:MAXPLOTDATALENGTH)
		isSnapshot(nullable:true)
	}
	
	static mapping = {
		table 'plot_data'
		userId column: 'user_id', index:'user_id_index'
		name column:'name', index:'name_index'
		jsonPlotData column:'json_plot_data'
		is_snapshot column: 'is_snapshot'
	}
	
	public static createOrReplace(User user, String name, String jsonPlotData, Boolean isSnapshot) {
		log.debug "PlotData.createOrReplace() userId:" + user.getId() + ", name:" + name + ", jsonPlotData(length):" + jsonPlotData.length() + ", isSnapshot:" + isSnapshot

		PlotData.executeUpdate("delete PlotData p where p.name = :name and p.userId = :userId",
			[name:name, userId:user.getId()]);
		
		return create(user, name, jsonPlotData, isSnapshot)
	}
	
	public static create(User user, String name, String jsonPlotData, Boolean isSnapshot) {
		log.debug "PlotData.create() userId:" + user.getId() + ", name:" + name + ", jsonPlotData(length):" + jsonPlotData.length() + ", isSnapshot:" + isSnapshot

		def plotData = JSON.parse(jsonPlotData)

		plotData.username = user.getUsername()
		
		return new PlotData(user.getId(), name, plotData.encodeAsJSON(), isSnapshot)
	}
	
	public static delete(PlotData plotData) {
		log.debug "PlotData.delete() plotDataId:" + plotData.getId()
		plotData.delete()
	}

	public static deleteId(long id) {
		log.debug "PlotData.deleteId() id:" + id
		PlotData plotData = PlotData.get(id)
		plotData.delete()
	}

	public PlotData() {
	}
	
	public PlotData(userId, name, jsonPlotData, isSnapshot) {
		Date now = new Date()
		
		this.userId = userId
		this.name = name
		this.created = now
		this.modified = now
		this.jsonPlotData = jsonPlotData
		this.isSnapshot = isSnapshot
	}
	
	/**
	 * Get data for the plot data lists
	 */
	def getJSONDesc() {
		return [
			id:this.id,
			name:this.name,
			created:this.created,
			modified:this.modified
		]
	}

	// for now, plots are either dynamic or snapshots
	def getIsDynamic() {
		return !getIsSnapshot()
	}
	
	String toString() {
		return "PlotData(id:" + getId() + ", name:" + name + ", created:" + Utils.dateToGMTString(created) + ", isSnapshot:" + isSnapshot + ")"
	}
}
