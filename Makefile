#Makefile
PLUGIN_NAME = NeuroRegister_
FIJI = fiji
FIJI_PLUGINS = /usr/lib/fiji/plugins
TEST_IMAGE = ~/data/201202_RegistAgain/0661-050323_1_sn/regist/moving.tif

all: jar

install: jar
	cp $(PLUGIN_NAME).jar $(FIJI_PLUGINS)

jar: _neblandmarks _swctools config
	cd $(PLUGIN_NAME); \
	jar cvf ../$(PLUGIN_NAME).jar *

_neblandmarks:
	$(FIJI) --javac $(PLUGIN_NAME)/neblandmarks/*.java

_swctools:
	$(FIJI) --javac $(PLUGIN_NAME)/swctools/*.java

config: ./NeuroRegister_/plugins.config

test:
	$(FIJI) $(TEST_IMAGE) &

clean:
	rm */*.class
	rm *~
	rm */*~

