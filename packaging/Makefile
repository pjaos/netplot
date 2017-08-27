#Get the versions for the programs to be built from the src code
NETPLOT_VER := $(shell python get_version.py ../src/netplot/PlotFrame.java)

OUTPUT_ROOT_PATH = output

all: netplot

#Remove all the output files
clean:
	rm -rf ../dist
	rm -rf $(OUTPUT_ROOT_PATH)/*.deb
	rm -rf $(OUTPUT_ROOT_PATH)/*.rpm
	rm -rf $(OUTPUT_ROOT_PATH)/*.tgz
	rm -rf $(OUTPUT_ROOT_PATH)/*.zip

#Build the python netplot package and output a deb, rpm and a tgz file.
netplot:
	sudo -u root rm -rf $(OUTPUT_ROOT_PATH)/netplot/linux
	rm -rf ../dist
	mkdir -p $(OUTPUT_ROOT_PATH)/netplot/linux/DEBIAN
	mkdir -p $(OUTPUT_ROOT_PATH)/netplot/linux/usr/local/bin/netplot_files/pjalib
	cp ../netplot_gui.jar $(OUTPUT_ROOT_PATH)/netplot/linux/usr/local/bin/netplot_files
	cp ../netplot_client_demo.jar $(OUTPUT_ROOT_PATH)/netplot/linux/usr/local/bin/netplot_files
	#This debian control file should not contain the Version: line
	cp netplot.control $(OUTPUT_ROOT_PATH)/netplot/linux/DEBIAN/control
	cp netplot.postinst $(OUTPUT_ROOT_PATH)/netplot/linux/DEBIAN/postinst
	cp netplot.prerm $(OUTPUT_ROOT_PATH)/netplot/linux/DEBIAN/prerm
	cp netplot.startup $(OUTPUT_ROOT_PATH)/netplot/linux/usr/local/bin/netplot
	cp netplot_demo.startup $(OUTPUT_ROOT_PATH)/netplot/linux/usr/local/bin/netplot_demo
	#Ensure the DEBIAN files have the correct permisions
	sudo chmod 755 $(OUTPUT_ROOT_PATH)/netplot/linux/DEBIAN/*
	sudo chmod 755 $(OUTPUT_ROOT_PATH)/netplot/linux/usr/local/bin/netplot
	sudo chmod 755 $(OUTPUT_ROOT_PATH)/netplot/linux/usr/local/bin/netplot_demo
	#Add the version to the debian control file
	echo 'Version: $(NETPLOT_VER)' >> $(OUTPUT_ROOT_PATH)/netplot/linux/DEBIAN/control
	#All files must be owned by root
	sudo chown -R root:root $(OUTPUT_ROOT_PATH)/netplot/linux/*
	#-Zgzip argument needed for backwards compatability (E.G Ubuntu 10.04)
	dpkg-deb -Zgzip -b $(OUTPUT_ROOT_PATH)/netplot/linux $(OUTPUT_ROOT_PATH)/netplot/netplot-$(NETPLOT_VER).deb 
	cd $(OUTPUT_ROOT_PATH)/netplot/ && sudo alien --to-tgz --scripts netplot-$(NETPLOT_VER).deb
	cd $(OUTPUT_ROOT_PATH)/netplot/ && sudo alien --to-rpm --scripts netplot-$(NETPLOT_VER).deb
	sudo -u root rm -rf $(OUTPUT_ROOT_PATH)/netplot/linux
