//http://www.masswerk.at
var TerminalDefaults = {
	// dimensions
	cols:80,
	rows:24,
	// appearance
	x:100,
	y:100,
	termDiv:'termDiv',
	bgColor:'#181818',
	frameColor:'#555555',
	frameWidth:1,
	rowHeight:15,
	blinkDelay:500,
	// css class
	fontClass:'term',
	// initial cursor mode
	crsrBlinkMode:false,
	crsrBlockMode:true,
	// key mapping
	DELisBS:false,
	printTab:true,
	printEuro:true,
	catchCtrlH:true,
	closeOnESC:true,
	// prevent consecutive history doublets
	historyUnique:false,
	// optional id
	id:0,
	// strings
	ps:'>',
	greeting:'%+r Terminal ready. %-r',
	// handlers
	handler:termDefaultHandler,
	ctrlHandler:null,
	initHandler:null,
	exitHandler:null
}

var Terminal = function(conf) {
	if (typeof conf != 'object') conf=new Object();
	else {
		for (var i in TerminalDefaults) {
			if (typeof conf[i] == 'undefined') conf[i]=TerminalDefaults[i];
		}
	}
	this.conf=conf;
	this.version='1.052 (original)';
	this.setInitValues();
}

Terminal.prototype.setInitValues=function() {
	this.id=this.conf.id;
	this.maxLines=this.conf.rows;
	this.maxCols=this.conf.cols;
	this.termDiv=this.conf.termDiv;
	this.crsrBlinkMode=this.conf.crsrBlinkMode;
	this.crsrBlockMode=this.conf.crsrBlockMode;
	this.blinkDelay=this.conf.blinkDelay;
	this.DELisBS=this.conf.DELisBS;
	this.printTab=this.conf.printTab;
	this.printEuro=this.conf.printEuro;
	this.catchCtrlH=this.conf.catchCtrlH;
	this.closeOnESC=this.conf.closeOnESC;
	this.historyUnique=this.conf.historyUnique;
	this.ps=this.conf.ps;
	this.closed=false;
	this.r;
	this.c;
	this.charBuf=new Array();
	this.styleBuf=new Array();
	this.scrollBuf=null;
	this.blinkBuffer=0;
	this.blinkTimer;
	this.cursoractive=false;
	this.lock=true;
	this.insert=false;
	this.charMode=false;
	this.rawMode=false;
	this.lineBuffer='';
	this.inputChar=0;
	this.lastLine='';
	this.guiCounter=0;
	this.history=new Array();
	this.histPtr=0;
	this.env=new Object();
	this.ns4ParentDoc=null;
	this.handler=this.conf.handler;
	this.ctrlHandler=this.conf.ctrlHandler;
	this.initHandler=this.conf.initHandler;
	this.exitHandler=this.conf.exitHandler;
}

function termDefaultHandler() {
	this.newLine();
	if (this.lineBuffer != '') {
		this.type('You typed: '+this.lineBuffer);
		this.newLine();
	}
	this.prompt();
}

Terminal.prototype.open=function() {
	if (this.termDivReady()) {
		if (!this.closed) this._makeTerm();
		this.init();
		return true;
	}
	else return false;
}

Terminal.prototype.close=function() {
	this.lock=true;
	this.cursorOff();
	if (this.exitHandler) this.exitHandler();
	TermGlobals.setVisible(this.termDiv,0);
	this.closed=true;
}

Terminal.prototype.init=function() {
	// wait for gui
	if (this.guiReady()) {
		this.guiCounter=0;
		// clean up at re-entry
		if (this.closed) {
			this.setInitValues();
		}
		this.clear();
		TermGlobals.setVisible(this.termDiv,1);
		TermGlobals.enableKeyboard(this);
		if (this.initHandler) {
			this.initHandler();
		}
		else {
			this.write(this.conf.greeting);
			this.newLine();
			this.prompt();
		}
	}
	else {
		this.guiCounter++;
		if (this.guiCounter>18000) {
			if (confirm('Terminal:\nYour browser hasn\'t responded for more than 2 minutes.\nRetry?')) this.guiCounter=0
			else return;
		};
		TermGlobals.termToInitialze=this;
		window.setTimeout('TermGlobals.termToInitialze.init()',200);
	}
}

Terminal.prototype.getRowArray=function(l,v) {
	var a=new Array();
	for (var i=0; i<l; i++) a[i]=v;
	return a;
}

Terminal.prototype.type=function(text,style) {
	for (var i=0; i<text.length; i++) {
		var ch=text.charCodeAt(i);
		if (!this.isPrintable(ch)) ch=94;
		this.charBuf[this.r][this.c]=ch;
		this.styleBuf[this.r][this.c]=(style)? style:0;
		var last_r=this.r;
		this._incCol();
		if (this.r!=last_r) this.redraw(last_r);
	}
	this.redraw(this.r)
}

Terminal.prototype.write=function(text,usemore) {
	// write to scroll buffer with markup
	// new line = '%n' prepare any strings or arrys first
	if (typeof text != 'object') {
		if (typeof text!='string') text=''+text;
		if (text.indexOf('\n')>=0) {
			var ta=text.split('\n');
			text=ta.join('%n');
		}
	}
	else {
		if (text.join) text=text.join('%n')
		else text=''+text;
	}
	this._sbInit(usemore);
	var chunks=text.split('%');
	var esc=(text.charAt(0)!='%');
	var style=0;
	for (var i=0; i<chunks.length; i++) {
		if (esc) {
			if (chunks[i].length>0) this._sbType(chunks[i],style)
			else if (i>0) this._sbType('%', style);
			esc=false;
		}
		else {
			var func=chunks[i].charAt(0);
			if ((chunks[i].length==0) && (i>0)) {
				this._sbType("%",style);
				esc=true;
			}
			else if (func=='n') {
				this._sbNewLine();
				if (chunks[i].length>1) this._sbType(chunks[i].substring(1),style);
			}
			else if (func=='+') {
				var opt=chunks[i].charAt(1);
				opt=opt.toLowerCase();
				if (opt=='p') style=0
				else if (opt=='r') style|=1
				else if (opt=='u') style|=2
				else if (opt=='i') style|=4
				else if (opt=='s') style|=8;
				if (chunks[i].length>2) this._sbType(chunks[i].substring(2),style);
			}
			else if (func=='-') {
				var opt=chunks[i].charAt(1);
				opt=opt.toLowerCase();
				if (opt=='p') style|=0
				else if (opt=='r') style&=~1
				else if (opt=='u') style&=~2
				else if (opt=='i') style&=~4
				else if (opt=='s') style&=~8;
				if (chunks[i].length>2) this._sbType(chunks[i].substring(2),style);
			}
			else if ((chunks[i].length>1) && (chunks[i].charAt(0)=='C') && (chunks[i].charAt(1)=='S')) {
				this.clear();
				this._sbInit();
				if (chunks[i].length>2) this._sbType(chunks[i].substring(2),style);
			}
			else {
				if (chunks[i].length>0) this._sbType(chunks[i],style);
			}
		}
	}
	this._sbOut();
}

Terminal.prototype._sbType=function(text,style) {
	// type to scroll buffer
	var sb=this.scrollBuf;
	for (var i=0; i<text.length; i++) {
		var ch=text.charCodeAt(i);
		if (!this.isPrintable(ch)) ch=94;
		sb.lines[sb.r][sb.c]=ch;
		sb.styles[sb.r][sb.c]=(style)? style:0;
		sb.c++;
		if (sb.c>=this.maxCols) this._sbNewLine();
	}
}

Terminal.prototype._sbNewLine=function() {
	var sb=this.scrollBuf;
	sb.r++;
	sb.c=0;
	sb.lines[sb.r]=this.getRowArray(this.conf.cols,0);
	sb.styles[sb.r]=this.getRowArray(this.conf.cols,0);
}


Terminal.prototype._sbInit=function(usemore) {
	var sb=this.scrollBuf=new Object();
	var sbl=sb.lines=new Array();
	var sbs=sb.styles=new Array();
	sb.more=usemore;
	sb.line=0;
	sb.status=0;
	sb.r=0;
	sb.c=this.c;
	sbl[0]=this.getRowArray(this.conf.cols,0);
	sbs[0]=this.getRowArray(this.conf.cols,0);
	for (var i=0; i<this.c; i++) {
		sbl[0][i]=this.charBuf[this.r][i];
		sbs[0][i]=this.styleBuf[this.r][i];
	}
}

Terminal.prototype._sbOut=function() {
	var sb=this.scrollBuf;
	var sbl=sb.lines;
	var sbs=sb.styles;
	var tcb=this.charBuf;
	var tsb=this.styleBuf;
	var ml=this.maxLines;
	var buflen=sbl.length;
	if (sb.more) {
		if (sb.status) {
			if (this.inputChar==TermGlobals.lcMoreKeyAbort) {
				/*this.r=ml-1;
				this.c=0;
				tcb[this.r]=this.getRowArray(this.maxLines,0);
				tsb[this.r]=this.getRowArray(this.maxLines,0);
				this.redraw(this.r);
				this.handler=sb.handler;
				this.charMode=false;
				this.inputChar=0;
				this.scrollBuf=null;
				this.prompt();
				return;*/
			}
			else if (this.inputChar==TermGlobals.lcMoreKeyContinue) {
				this.clear();
			}
			else {
				return;
			}
		}
		else {
			if (this.r>=ml-1) this.clear();
		}
	}
	if (this.r+buflen-sb.line<=ml) {
		for (var i=sb.line; i<buflen; i++) {
			var r=this.r+i-sb.line;
			tcb[r]=sbl[i];
			tsb[r]=sbs[i];
			this.redraw(r);
		}
		this.r+=sb.r-sb.line;
		this.c=sb.c;
		if (sb.more) {
			if (sb.status) this.handler=sb.handler;
			this.charMode=false;
			this.inputChar=0;
			this.scrollBuf=null;
			this.prompt();
			return;
		}
	}
	else if (sb.more) {
		ml--;
		if (sb.status==0) {
			sb.handler=this.handler;
			this.handler=this._sbOut;
			this.charMode=true;
			sb.status=1;
		}
		if (this.r) {
			var ofs=ml-this.r;
			for (var i=sb.line; i<ofs; i++) {
				var r=this.r+i-sb.line;
				tcb[r]=sbl[i];
				tsb[r]=sbs[i];
				this.redraw(r);
			}
		}
		else {
			var ofs=sb.line+ml;
			for (var i=sb.line; i<ofs; i++) {
				var r=this.r+i-sb.line;
				tcb[r]=sbl[i];
				tsb[r]=sbs[i];
				this.redraw(r);
			}
		}
		sb.line=ofs;
		this.r=ml;
		this.c=0;
		this.type(TermGlobals.lcMorePrompt1, TermGlobals.lcMorePromtp1Style);
		this.type(TermGlobals.lcMorePrompt2, TermGlobals.lcMorePrompt2Style);
		this.lock=false;
		return;
	}
	else if (buflen>=ml) {
		var ofs=buflen-ml;
		for (var i=0; i<ml; i++) {
			var r=ofs+i;
			tcb[i]=sbl[r];
			tsb[i]=sbs[r];
			this.redraw(i);
		}
		this.r=ml-1;
		this.c=sb.c;
	}
	else {
		var dr=ml-buflen;
		var ofs=this.r-dr;
		for (var i=0; i<dr; i++) {
			var r=ofs+i;
			for (var c=0; c<this.maxCols; c++) {
				tcb[i][c]=tcb[r][c];
				tsb[i][c]=tsb[r][c];
			}
			this.redraw(i);
		}
		for (var i=0; i<buflen; i++) {
			var r=dr+i;
			tcb[r]=sbl[i];
			tsb[r]=sbs[i];
			this.redraw(r);
		}
		this.r=ml-1;
		this.c=sb.c;
	}
	this.scrollBuf=null;
}

// basic console output

Terminal.prototype.typeAt=function(r,c,text,style) {
	var tr1=this.r;
	var tc1=this.c;
	this.cursorSet(r,c);
	for (var i=0; i<text.length; i++) {
		var ch=text.charCodeAt(i);
		if (!this.isPrintable(ch)) ch=94;
		this.charBuf[this.r][this.c]=ch;
		this.styleBuf[this.r][this.c]=(style)? style:0;
		var last_r=this.r;
		this._incCol();
		if (this.r!=last_r) this.redraw(last_r);
	}
	this.redraw(this.r);
	this.r=tr1;
	this.c=tc1;
}

Terminal.prototype.statusLine = function(text,style,offset) {
	var ch,r;
	style=((style) && (!isNaN(style)))? parseInt(style)&15:0;
	if ((offset) && (offset>0)) r=this.conf.rows-offset
	else r=this.conf.rows-1;
	for (var i=0; i<this.conf.cols; i++) {
		if (i<text.length) {
			ch=text.charCodeAt(i);
			if (!this.isPrintable(ch)) ch = 94;
		}
		else ch=0;
		this.charBuf[r][i]=ch;
		this.styleBuf[r][i]=style;
	}
	this.redraw(r);
}

Terminal.prototype.printRowFromString = function(r,text,style) {
	var ch;
	style=((style) && (!isNaN(style)))? parseInt(style)&15:0;
	if ((r>=0) && (r<this.maxLines)) {
		if (typeof text != 'string') text=''+text;
		for (var i=0; i<this.conf.cols; i++) {
			if (i<text.length) {
				ch=text.charCodeAt(i);
				if (!this.isPrintable(ch)) ch = 94;
			}
			else ch=0;
			this.charBuf[r][i]=ch;
			this.styleBuf[r][i]=style;
		}
		this.redraw(r);
	}
}

Terminal.prototype.setChar=function(ch,r,c,style) {
	this.charBuf[r][c]=ch;
	this.styleBuf[this.r][this.c]=(style)? style:0;
	this.redraw(r);
}

Terminal.prototype._charOut=function(ch, style) {
	this.charBuf[this.r][this.c]=ch;
	this.styleBuf[this.r][this.c]=(style)? style:0;
	this.redraw(this.r);
	this._incCol();
}

Terminal.prototype._incCol=function() {
	this.c++;
	if (this.c>=this.maxCols) {
		this.c=0;
		this._incRow();
	}
}

Terminal.prototype._incRow=function() {
	this.r++;
	if (this.r>=this.maxLines) {
		this._scrollLines(0,this.maxLines);
		this.r=this.maxLines-1;
	}
}

Terminal.prototype._scrollLines=function(start, end) {
	window.status='Scrolling lines ...';
	start++;
	for (var ri=start; ri<end; ri++) {
		var rt=ri-1;
		this.charBuf[rt]=this.charBuf[ri];
		this.styleBuf[rt]=this.styleBuf[ri];
	}
	// clear last line
	var rt=end-1;
	this.charBuf[rt]=this.getRowArray(this.conf.cols,0);
	this.styleBuf[rt]=this.getRowArray(this.conf.cols,0);
	this.redraw(rt);
	for (var r=end-1; r>=start; r--) this.redraw(r-1);
	window.status='';
}

Terminal.prototype.newLine=function() {
	this.c=0;
	this._incRow();
}

Terminal.prototype.clear=function() {
	window.status='Clearing display ...';
	this.cursorOff();
	this.insert=false;
	for (var ri=0; ri<this.maxLines; ri++) {
		this.charBuf[ri]=this.getRowArray(this.conf.cols,0);
		this.styleBuf[ri]=this.getRowArray(this.conf.cols,0);
		this.redraw(ri);
	}
	this.r=0;
	this.c=0;
	window.status='';
}

Terminal.prototype.reset=function() {
	if (this.lock) return;
	this.lock=true;
	this.rawMode=false;
	this.charMode=false;
	this.maxLines=this.conf.rows;
	this.maxCols=this.conf.cols;
	this.lastLine='';
	this.lineBuffer='';
	this.inputChar=0;
	this.clear();
}

Terminal.prototype.cursorSet=function(r,c) {
	var crsron=this.cursoractive;
	if (crsron) this.cursorOff();
	this.r=r%this.maxLines;
	this.c=c%this.maxCols;
	this._cursorReset(crsron);
}

Terminal.prototype._cursorReset=function(crsron) {
	if (crsron) this.cursorOn()
	else {
		this.blinkBuffer=this.styleBuf[this.r][this.c];
	}
}

Terminal.prototype.cursorOn=function() {
	if (this.blinkTimer) clearTimeout(this.blinkTimer);
	this.blinkBuffer=this.styleBuf[this.r][this.c];
	this._cursorBlink();
	this.cursoractive=true;
}

Terminal.prototype.cursorOff=function() {
	if (this.blinkTimer) clearTimeout(this.blinkTimer);
	if (this.cursoractive) {
		this.styleBuf[this.r][this.c]=this.blinkBuffer;
		this.redraw(this.r);
		this.cursoractive=false;
	}
}

Terminal.prototype._cursorBlink=function() {
	if (this.blinkTimer) clearTimeout(this.blinkTimer);
	if (this == TermGlobals.activeTerm) {
		if (this.crsrBlockMode) {
			this.styleBuf[this.r][this.c]=(this.styleBuf[this.r][this.c]&1)?
				this.styleBuf[this.r][this.c]&254:this.styleBuf[this.r][this.c]|1;
		}
		else {
			this.styleBuf[this.r][this.c]=(this.styleBuf[this.r][this.c]&2)?
				this.styleBuf[this.r][this.c]&253:this.styleBuf[this.r][this.c]|2;
		}
		this.redraw(this.r);
	}
	if (this.crsrBlinkMode) this.blinkTimer=setTimeout('TermGlobals.activeTerm._cursorBlink()', this.blinkDelay);
}

Terminal.prototype.cursorLeft=function() {
	var crsron=this.cursoractive;
	if (crsron) this.cursorOff();
	var r=this.r;
	var c=this.c;
	if (c>0) c--
	else if (r>0) {
		c=this.maxCols-1;
		r--;
	}
	if (this.isPrintable(this.charBuf[r][c])) {
		this.r=r;
		this.c=c;
	}
	this.insert=true;
	this._cursorReset(crsron);
}

Terminal.prototype.cursorRight=function() {
	var crsron=this.cursoractive;
	if (crsron) this.cursorOff();
	var r=this.r;
	var c=this.c;
	if (c<this.maxCols-1) c++
	else if (r<this.maxLines-1) {
		c=0;
		r++;
	}
	if (!this.isPrintable(this.charBuf[r][c])) {
		this.insert=false;
	}
	if (this.isPrintable(this.charBuf[this.r][this.c])) {
		this.r=r;
		this.c=c;
	}
	this._cursorReset(crsron);
}

Terminal.prototype.backspace=function() {
	var crsron=this.cursoractive;
	if (crsron) this.cursorOff();
	var r=this.r;
	var c=this.c;
	if (c>0) c--
	else if (r>0) {
		c=this.maxCols-1;
		r--;
	};
	if (this.isPrintable(this.charBuf[r][c])) {
		this._scrollLeft(r, c);
		this.r=r;
		this.c=c;
	};	
	this._cursorReset(crsron);
}

Terminal.prototype.fwdDelete=function() {
	var crsron=this.cursoractive;
	if (crsron) this.cursorOff();
	if (this.isPrintable(this.charBuf[this.r][this.c])) {
		this._scrollLeft(this.r,this.c);
		if (!this.isPrintable(this.charBuf[this.r][this.c])) this.insert=false;
	}
	this._cursorReset(crsron);
}

Terminal.prototype.prompt=function() {
	this.lock=true;
	if (this.c>0) this.newLine();
	this.type(this.ps);
	this._charOut(1);
	this.lock=false;
	this.cursorOn();
}

Terminal.prototype._scrollLeft=function(r,c) {
	var rows=new Array();
	rows[0]=r;
	while (this.isPrintable(this.charBuf[r][c])) {
		var ri=r;
		var ci=c+1;
		if (ci==this.maxCols) {
			if (ri<this.maxLines-1) {
				ci=0;
				ri++;
				rows[rows.length]=ri;
			}
			else {
				break;
			}
		}
		this.charBuf[r][c]=this.charBuf[ri][ci];
		this.styleBuf[r][c]=this.styleBuf[ri][ci];
		c=ci;
		r=ri;
	}
	if (this.charBuf[r][c]!=0) this.charBuf[r][c]=0;
	for (var i=0; i<rows.length; i++) this.redraw(rows[i]);
}

Terminal.prototype._scrollRight=function(r,c) {
	var rows=new Array();
	var end=this._getLineEnd(r,c);
	var ri=end[0];
	var ci=end[1];
	if ((ci==this.maxCols-1) && (ri==this.maxLines-1)) {
		if (r==0) return;
		this._scrollLines(0,this.maxLines);
		this.r--;
		r--;
		ri--;
	}
	rows[r]=1;
	while (this.isPrintable(this.charBuf[ri][ci])) {
		var rt=ri;
		var ct=ci+1;
		if (ct==this.maxCols) {
			ct=0;
			rt++;
			rows[rt]=1;
		}
		this.charBuf[rt][ct]=this.charBuf[ri][ci];
		this.styleBuf[rt][ct]=this.styleBuf[ri][ci];
		if ((ri==r) && (ci==c)) break;
		ci--;
		if (ci<0) {
			ci=this.maxCols-1;
			ri--;
			rows[ri]=1;
		}
	}
	for (var i=r; i<this.maxLines; i++) {
		if (rows[i]) this.redraw(i);
	}
}

Terminal.prototype._getLineEnd=function(r,c) {
	if (!this.isPrintable(this.charBuf[r][c])) {
		c--;
		if (c<0) {
			if (r>0) {
				r--;
				c=this.maxCols-1;
			}
			else {
				c=0;
			}
		}
	}
	if (this.isPrintable(this.charBuf[r][c])) {
		while (true) {
			var ri=r;
			var ci=c+1;
			if (ci==this.maxCols) {
				if (ri<this.maxLines-1) {
					ri++;
					ci=0;
				}
				else {
					break;
				}
			}
			if (!this.isPrintable(this.charBuf[ri][ci])) break;
			c=ci;
			r=ri;
		}
	}
	return [r,c];
}

Terminal.prototype._getLineStart=function(r,c) {
	// not used by now, just in case anyone needs this ...
	var ci, ri;
	if (!this.isPrintable(this.charBuf[r][c])) {
		ci=c-1;
		ri=r;
		if (ci<0) {
			if (ri==0) return [0,0];
			ci=this.maxCols-1;
			ri--;
		}
		if (!this.isPrintable(this.charBuf[ri][ci])) return [r,c]
		else {
			r=ri;
			c=ci;
		}
	}
	while (true) {
		var ri=r;
		var ci=c-1;
		if (ci<0) {
			if (ri==0) break;
			ci=this.maxCols-1;
			ri--;
		}
		if (!this.isPrintable(this.charBuf[ri][ci])) break;;
		r=ri;
		c=ci;
	}
	return [r,c];
}

Terminal.prototype._getLine=function() {
	var end=this._getLineEnd(this.r,this.c);
	var r=end[0];
	var c=end[1];
	var line=new Array();
	while (this.isPrintable(this.charBuf[r][c])) {
		line[line.length]=String.fromCharCode(this.charBuf[r][c]);
		if (c>0) c--
		else if (r>0) {
			c=this.maxCols-1;
			r--;
		}
		else break;
	}
	line.reverse();
	return line.join('');
}

Terminal.prototype._clearLine=function() {
	var end=this._getLineEnd(this.r,this.c);
	var r=end[0];
	var c=end[1];
	var line='';
	while (this.isPrintable(this.charBuf[r][c])) {
		this.charBuf[r][c]=0;
		if (c>0) {
			c--;
		}
		else if (r>0) {
			this.redraw(r);
			c=this.maxCols-1;
			r--;
		}
		else break;
	}
	if (r!=end[0]) this.redraw(r);
	c++;
	this.cursorSet(r,c);
	this.insert=false;
}

Terminal.prototype.isPrintable=function(ch, unicodePage1only) {
	if ((unicodePage1only) && (ch>255)) {
		return ((ch==termKey.EURO) && (this.printEuro))? true:false;
	}
	return (
		((ch>=32) && (ch!=termKey.DEL)) ||
		((this.printTab) && (ch==termKey.TAB))
	);
}

// keyboard focus

Terminal.prototype.focus=function() {
	TermGlobals.activeTerm=this;
}

// global store and functions

var TermGlobals={
	termToInitialze:null,
	activeTerm:null,
	kbdEnabled:false,
	keylock:false,
	lcMorePrompt1: 'NEXT',
	lcMorePromtp1Style: 1,
	lcMorePrompt2: ' (Type: space to continue)',
	lcMorePrompt2Style: 0,
	lcMoreKeyAbort: 113,
	lcMoreKeyContinue: 32
};

// keybard focus

TermGlobals.setFocus=function(termref) {
	TermGlobals.activeTerm=termref;
}

// text related

TermGlobals.normalize=function(n,m) {
	var s=''+n;
	while (s.length<m) s='0'+s;
	return s;
}

TermGlobals.fillLeft=function(t,n) {
	if (typeof t != 'string') t=''+t;
	while (t.length<n) t=' '+t;
	return t;
}

TermGlobals.center=function(t,l) {
	var s='';
	for (var i=t.length; i<l; i+=2) s+=' ';
	return s+t;
}

TermGlobals.stringReplace=function(s1,s2,t) {
	var l1=s1.length;
	var l2=s2.length;
	var ofs=t.indexOf(s1);
	while (ofs>=0) {
		t=t.substring(0,ofs)+s2+t.substring(ofs+l1);
		ofs=t.indexOf(s1,ofs+l2);
	}
	return t;
}

// keyboard

var termKey= {
	// special key codes
	'NUL': 0x00,
	'SOH': 0x01,
	'STX': 0x02,
	'ETX': 0x03,
	'EOT': 0x04,
	'ENQ': 0x05,
	'ACK': 0x06,
	'BEL': 0x07,
	'BS': 0x08,
	'HT': 0x09,
	'TAB': 0x09,
	'LF': 0x0A,
	'VT': 0x0B,
	'FF': 0x0C,
	'CR': 0x0D,
	'SO': 0x0E,
	'SI': 0x0F,
	'DLE': 0x10,
	'DC1': 0x11,
	'DC2': 0x12,
	'DC3': 0x13,
	'DC4': 0x14,
	'NAK': 0x15,
	'SYN': 0x16,
	'ETB': 0x17,
	'CAN': 0x18,
	'EM': 0x19,
	'SUB': 0x1A,
	'ESC': 0x1B,
	'IS4': 0x1C,
	'IS3': 0x1D,
	'IS2': 0x1E,
	'IS1': 0x1F,
	'DEL': 0x7F,
	// other specials
	'EURO': 0x20AC,
	// cursor mapping
	'LEFT': 0x1C,
	'RIGHT': 0x1D,
	'UP': 0x1E,
	'DOWN': 0x1F
};

var termDomKeyRef = {
	DOM_VK_LEFT: termKey.LEFT,
	DOM_VK_RIGHT: termKey.RIGHT,
	DOM_VK_UP: termKey.UP,
	DOM_VK_DOWN: termKey.DOWN,
	DOM_VK_BACK_SPACE: termKey.BS,
	DOM_VK_RETURN: termKey.CR,
	DOM_VK_ENTER: termKey.CR,
	DOM_VK_ESCAPE: termKey.ESC,
	DOM_VK_DELETE: termKey.DEL,
	DOM_VK_TAB: termKey.TAB
};

TermGlobals.enableKeyboard=function(term) {
	if (!this.kbdEnabled) {
		if (document.addEventListener) document.addEventListener("keypress", this.keyHandler, true)
		else {
			if ((self.Event) && (self.Event.KEYPRESS)) document.captureEvents(Event.KEYPRESS);
			document.onkeypress = this.keyHandler;
		}
		window.document.onkeydown=this.keyFix;
		this.kbdEnabled=true;
	}
	this.activeTerm=term;
}

TermGlobals.keyFix=function() {
	if ((TermGlobals.keylock) || (TermGlobals.activeTerm.lock)) return true;
	if (window.event) {
		var ch=window.event.keyCode;
		var e=window.event;
		if (e.DOM_VK_UP) {
			for (var i in termDomKeyRef) {
				if ((e[i]) && (ch == e[i])) {
					this.keyHandler({which:termDomKeyRef[i],_remapped:true});
					if (e.preventDefault) e.preventDefault();
					if (e.stopPropagation) e.stopPropagation();
					e.cancleBubble=true;
					return false;
				}
			}
			e.cancleBubble=false;
			return true;
		}
		else {
			// no DOM support
			if (ch==8) TermGlobals.keyHandler({which:termKey.BS,_remapped:true})
			else if (ch==9) TermGlobals.keyHandler({which:termKey.TAB,_remapped:true})
			else if (ch==37) TermGlobals.keyHandler({which:termKey.LEFT,_remapped:true})
			else if (ch==39) TermGlobals.keyHandler({which:termKey.RIGHT,_remapped:true})
			else if (ch==38) TermGlobals.keyHandler({which:termKey.UP,_remapped:true})
			else if (ch==40) TermGlobals.keyHandler({which:termKey.DOWN,_remapped:true})
			else if (ch==127) TermGlobals.keyHandler({which:termKey.DEL,_remapped:true})
			else if ((ch>=57373) && (ch<=57376)) {
				if (ch==57373) TermGlobals.keyHandler({which:termKey.UP,_remapped:true})
				else if (ch==57374) TermGlobals.keyHandler({which:termKey.DOWN,_remapped:true})
				else if (ch==57375) TermGlobals.keyHandler({which:termKey.LEFT,_remapped:true})
				else if (ch==57376) TermGlobals.keyHandler({which:termKey.RIGHT,_remapped:true});
			}
			else {
				e.cancleBubble=false;
				return true;
			}
			if (e.preventDefault) e.preventDefault();
			if (e.stopPropagation) e.stopPropagation();
			e.cancleBubble=true;
			return false;
		}
	}
}

TermGlobals.keyHandler=function(e) {
	var term=TermGlobals.activeTerm;
	if ((TermGlobals.keylock) || (term.lock)) return true;
	if ((window.event) && (window.event.preventDefault)) window.event.preventDefault()
	else if ((e) && (e.preventDefault)) e.preventDefault();
	if ((window.event) && (window.event.stopPropagation)) window.event.stopPropagation()
	else if ((e) && (e.stopPropagation)) e.stopPropagation();
	var ch;
	var ctrl=false;
	var shft=false;
	var remapped=false;
	if (e) {
		ch=e.which;
		ctrl=((e.ctrlKey) || (e.modifiers==2));
		shft=((e.shiftKey) || (e.modifiers==4));
		if (e._remapped) {
			remapped=true;
			if (window.event) {
				ctrl=((ctrl) || (window.event.ctrlKey));
				shft=((shft) || (window.event.shiftKey));
			}
		}
	}
	else if (window.event) {
		ch=window.event.keyCode;
		ctrl=(window.event.ctrlKey);
		shft=(window.event.shiftKey);
	}
	else {
		return true;
	}
	if ((ch=='') && (remapped==false)) {
		// map specials
		if (e==null) e=window.event;
		if ((e.charCode==0) && (e.keyCode)) {
			if (e.DOM_VK_UP) {
				for (var i in termDomKeyRef) {
					if ((e[i]) && (e.keyCode == e[i])) {
						ch=termDomKeyRef[i];
						break;
					}
				}
			}
			else {
				// NS4
				if (e.keyCode==28) ch=termKey.LEFT
				else if (e.keyCode==29) ch=termKey.RIGHT
				else if (e.keyCode==30) ch=termKey.UP
				else if (e.keyCode==31) ch=termKey.DOWN
				// Mozilla alike but no DOM support
				else if (e.keyCode==37) ch=termKey.LEFT
				else if (e.keyCode==39) ch=termKey.RIGHT
				else if (e.keyCode==38) ch=termKey.UP
				else if (e.keyCode==40) ch=termKey.DOWN
				// just to have the TAB mapping here too
				else if (e.keyCode==9) ch=termKey.TAB;
			}
		}
	}
	// key actions
	if (term.charMode) {
		term.insert=false;
		term.inputChar=ch;
		term.lineBuffer='';
		term.handler();
		if ((ch<=32) && (window.event)) window.event.cancleBubble=true;
		return false;
	}
	if (!ctrl) {
		// special keys
		if (ch==termKey.CR) {
			term.lock=true;
			term.cursorOff();
			term.insert=false;
			if (term.rawMode) {
				term.lineBuffer=term.lastLine;
			}
			else {
				term.lineBuffer=term._getLine();
				if (
				    (term.lineBuffer!='') && ((!term.historyUnique) ||
				    (term.history.length==0) ||
				    (term.lineBuffer!=term.history[term.history.length-1]))
				   ) {
					term.history[term.history.length]=term.lineBuffer;
				}
				term.histPtr=term.history.length;
			}
			term.lastLine='';
			term.inputChar=0;
			term.handler();
			if (window.event) window.event.cancleBubble=true;
			return false;
		}
		/*else if (ch==termKey.ESC) {
			if (term.conf.closeOnESC) term.close();
			if (window.event) window.event.cancleBubble=true;
			return false;
		}*/
		if ((ch<32) && (term.rawMode)) {
			if (window.event) window.event.cancleBubble=true;
			return false;
		}
		else {
			if (ch==termKey.LEFT) {
				term.cursorLeft();
				if (window.event) window.event.cancleBubble=true;
				return false;
			}
			else if (ch==termKey.RIGHT) {
				term.cursorRight();
				if (window.event) window.event.cancleBubble=true;
				return false;
			}
			else if (ch==termKey.UP) {
				term.cursorOff();
				if (term.histPtr==term.history.length) term.lastLine=term._getLine();
				term._clearLine();
				if ((term.history.length) && (term.histPtr>=0)) {
					if (term.histPtr>0) term.histPtr--;
					term.type(term.history[term.histPtr]);
				}
				else if (term.lastLine) term.type(term.lastLine);
				term.cursorOn();
				if (window.event) window.event.cancleBubble=true;
				return false;
			}
			else if (ch==termKey.DOWN) {
				term.cursorOff();
				if (term.histPtr==term.history.length) term.lastLine=term._getLine();
				term._clearLine();
				if ((term.history.length) && (term.histPtr<=term.history.length)) {
					if (term.histPtr<term.history.length) term.histPtr++;
					if (term.histPtr<term.history.length) term.type(term.history[term.histPtr])
					else if (term.lastLine) term.type(term.lastLine);
				}
				else if (term.lastLine) term.type(term.lastLine);
				term.cursorOn();
				if (window.event) window.event.cancleBubble=true;
				return false;
			}
			else if (ch==termKey.BS) {
				term.backspace();
				if (window.event) window.event.cancleBubble=true;
				return false;
			}
			else if (ch==termKey.DEL) {
				if (term.DELisBS) term.backspace()
				else term.fwdDelete();
				if (window.event) window.event.cancleBubble=true;
				return false;
			}
		}
	}
	if (term.rawMode) {
		if (term.isPrintable(ch)) {
			term.lastLine+=String.fromCharCode(ch);
		}
		if ((ch==32) && (window.event)) window.event.cancleBubble=true
		else if ((window.opera) && (window.event)) window.event.cancleBubble=true;
		return false;
	}
	else {
		if ((term.conf.catchCtrlH) && ((ch==termKey.BS) || ((ctrl) && (ch==72)))) {
			// catch ^H
			term.backspace();
			if (window.event) window.event.cancleBubble=true;
			return false;
		}
		else if ((term.ctrlHandler) && ((ch<32) || ((ctrl) && (term.isPrintable(ch,true))))) {
			if (((ch>=65) && (ch<=96)) || (ch==63)) {
				// remap canonical
				if (ch==63) ch=31
				else ch-=64;
			}
			term.inputChar=ch;
			term.ctrlHandler();
			if (window.event) window.event.cancleBubble=true;
			return false;
		}
		else if ((ctrl) || (!term.isPrintable(ch,true))) {
			if (window.event) window.event.cancleBubble=true;
			return false;
		}
		else if (term.isPrintable(ch,true)) {
			if (term.blinkTimer) clearTimeout(term.blinkTimer);
			if (term.insert) {
				term.cursorOff();
				term._scrollRight(term.r,term.c);
			}
			term._charOut(ch);
			term.cursorOn();
			if ((ch==32) && (window.event)) window.event.cancleBubble=true
			else if ((window.opera) && (window.event)) window.event.cancleBubble=true;
			return false;
		}
	}
	return true;
}

// term gui

TermGlobals.hasSubDivs=false;
TermGlobals.hasLayers=false;
TermGlobals.termStringStart='';
TermGlobals.termStringEnd='';

TermGlobals.termSpecials=new Array();
TermGlobals.termSpecials[0]='&nbsp;';
TermGlobals.termSpecials[1]='&nbsp;';
TermGlobals.termSpecials[9]='&nbsp;';
TermGlobals.termSpecials[32]='&nbsp;';
TermGlobals.termSpecials[34]='&quot;';
TermGlobals.termSpecials[38]='&amp;';
TermGlobals.termSpecials[60]='&lt;';
TermGlobals.termSpecials[62]='&gt;';
TermGlobals.termSpecials[127]='&loz;';
TermGlobals.termSpecials[0x20AC]='&euro;';

TermGlobals.termStyles=new Array(1,2,4,8);
TermGlobals.termStyleOpen=new Array();
TermGlobals.termStyleClose=new Array();
TermGlobals.termStyleOpen[1]='<SPAN CLASS="termReverse">';
TermGlobals.termStyleClose[1]='<\/SPAN>';
TermGlobals.termStyleOpen[2]='<U>';
TermGlobals.termStyleClose[2]='<\/U>';
TermGlobals.termStyleOpen[4]='<I>';
TermGlobals.termStyleClose[4]='<\/I>';
TermGlobals.termStyleOpen[8]='<STRIKE>';
TermGlobals.termStyleClose[8]='<\/STRIKE>';

Terminal.prototype._makeTerm=function() {
	window.status='Building terminal ...';
	TermGlobals.hasLayers=(document.layers)? true:false;
	TermGlobals.hasSubDivs=(navigator.userAgent.indexOf('Gecko')<0);
	var divPrefix=this.termDiv+'_r';
	var s='';
	s+='<TABLE BORDER="0" CELLSPACING="0" CELLPADDING="'+this.conf.frameWidth+'">\n';
	s+='<TR><TD BGCOLOR="'+this.conf.frameColor+'"><TABLE BORDER="0" CELLSPACING="0" CELLPADDING="2"><TR><TD  BGCOLOR="'+this.conf.bgColor+'"><TABLE BORDER="0" CELLSPACING="0" CELLPADDING="0">\n';
	var rstr='';
	for (var c=0; c<this.conf.cols; c++) rstr+='&nbsp;';
	for (var r=0; r<this.conf.rows; r++) {
		var termid=((TermGlobals.hasLayers) || (TermGlobals.hasSubDivs))? '' : ' ID="'+divPrefix+r+'"';
		s+='<TR><TD NOWRAP HEIGHT="'+this.conf.rowHeight+'"'+termid+' CLASS="'+this.conf.fontClass+'">'+rstr+'<\/TD><\/TR>\n';
	}
	s+='<\/TABLE><\/TD><\/TR>\n';
	s+='<\/TABLE><\/TD><\/TR>\n';
	s+='<\/TABLE>\n';
	var termOffset=2+this.conf.frameWidth;
	if (TermGlobals.hasLayers) {
		for (var r=0; r<this.conf.rows; r++) {
			s+='<LAYER NAME="'+divPrefix+r+'" TOP="'+(termOffset+r*this.conf.rowHeight)+'" LEFT="'+termOffset+'" CLASS="'+this.conf.fontClass+'"><\/LAYER>\n';
		}
		this.ns4ParentDoc=document.layers[this.termDiv].document;
		TermGlobals.termStringStart='<TABLE BORDER="0" CELLSPACING="0" CELLPADDING="0"><TR><TD NOWRAP HEIGHT="'+this.conf.rowHeight+'" CLASS="'+this.conf.fontClass+'">';
		TermGlobals.termStringEnd='<\/TD><\/TR><\/TABLE>';
	}
	else if (TermGlobals.hasSubDivs) {
		for (var r=0; r<this.conf.rows; r++) {
			s+='<DIV ID="'+divPrefix+r+'" STYLE="position:absolute; top:'+(termOffset+r*this.conf.rowHeight)+'px; left: '+termOffset+'px;" CLASS="'+this.conf.fontClass+'"><\/DIV>\n';
		}
		TermGlobals.termStringStart='<TABLE BORDER="0" CELLSPACING="0" CELLPADDING="0"><TR><TD NOWRAP HEIGHT="'+this.conf.rowHeight+'" CLASS="'+this.conf.fontClass+'">';
		TermGlobals.termStringEnd='<\/TD><\/TR><\/TABLE>';
	}
	TermGlobals.writeElement(this.termDiv,s);
	TermGlobals.setElementXY(this.termDiv,this.conf.x,this.conf.y);
	TermGlobals.setVisible(this.termDiv,1);
	window.status='';
}

Terminal.prototype.moveTo=function(x,y) {
	TermGlobals.setElementXY(this.termDiv,x,y);
}

Terminal.prototype.resizeTo=function(x,y) {
	if (this.termDivReady()) {
		x=parseInt(x,10);
		y=parseInt(y,10);
		if ((isNaN(x)) || (isNaN(y)) || (x<4) || (y<2)) return false;
		this.maxCols=this.conf.cols=x;
		this.maxLines=this.conf.rows=y;
		this._makeTerm();
		this.clear();
		return true;
	}
	else return false;
}

Terminal.prototype.redraw=function(r) {
	var s=TermGlobals.termStringStart;
	var curStyle=0;
	var tstls=TermGlobals.termStyles;
	var tscls=TermGlobals.termStyleClose;
	var tsopn=TermGlobals.termStyleOpen;
	var tspcl=TermGlobals.termSpecials;
	var t_cb=this.charBuf;
	var t_sb=this.styleBuf;
	for (var i=0; i<this.conf.cols; i++) {
		var c=t_cb[r][i];
		var cs=t_sb[r][i];
		if (cs!=curStyle) {
			if (curStyle) {
				for (var k=tstls.length-1; k>=0; k--) {
					var st=tstls[k];
					if (curStyle&st) s+=tscls[st];
				}
			}
			curStyle=cs;
			for (var k=0; k<tstls.length; k++) {
				var st=tstls[k];
				if (curStyle&st) s+=tsopn[st];
			}
		}
		s+= (tspcl[c])? tspcl[c] : String.fromCharCode(c);
	}
	if (curStyle>0) {
		for (var k=tstls.length-1; k>=0; k--) {
			var st=tstls[k];
			if (curStyle&st) s+=tscls[st];
		}
	}
	s+=TermGlobals.termStringEnd;
	TermGlobals.writeElement(this.termDiv+'_r'+r,s,this.ns4ParentDoc);
}

Terminal.prototype.guiReady=function() {
	ready=true;
	if (TermGlobals.guiElementsReady(this.termDiv, self.document)) {
		for (var r=0; r<this.conf.rows; r++) {
			if (TermGlobals.guiElementsReady(this.termDiv+'_r'+r,this.ns4ParentDoc)==false) {
				ready=false;
				break;
			}
		}
	}
	else ready=false;
	return ready;
}

Terminal.prototype.termDivReady=function() {
	if (document.layers) {
		return (document.layers[this.termDiv])? true:false;
	}
	else if (document.getElementById) {
		return (document.getElementById(this.termDiv))? true:false;
	}
	else if (document.all) {
		return (document.all[this.termDiv])? true:false;
	}
	else {
		return false;
	}
}

Terminal.prototype.getDimensions=function() {
	var w=0;
	var h=0;
	var d=this.termDiv;
	if (document.layers) {
		if (document.layers[d]) {
			w=document.layers[d].clip.right;
			h=document.layers[d].clip.bottom;
		}
	}
	else if (document.getElementById) {
		var obj=document.getElementById(d);
		if ((obj) && (obj.firstChild)) {
			w=parseInt(obj.firstChild.offsetWidth,10);
			h=parseInt(obj.firstChild.offsetHeight,10);
        }
		else if ((obj) && (obj.children) && (obj.children[0])) {
			w=parseInt(obj.children[0].offsetWidth,10);
			h=parseInt(obj.children[0].offsetHeight,10);
        }
	}
	else if (document.all) {
		var obj=document.all[d];
		if ((obj) && (obj.children) && (obj.children[0])) {
			w=parseInt(obj.children[0].offsetWidth,10);
			h=parseInt(obj.children[0].offsetHeight,10);
        }
	}
	return { width: w, height: h };
}

// basic dynamics

TermGlobals.writeElement=function(e,t,d) {
	if (document.layers) {
		var doc=(d)? d : self.document;
		doc.layers[e].document.open();
		doc.layers[e].document.write(t);
		doc.layers[e].document.close();
	}
	else if (document.getElementById) {
		var obj=document.getElementById(e);
		obj.innerHTML=t;
	}
	else if (document.all) {
		document.all[e].innerHTML=t;
	}
}

TermGlobals.setElementXY=function(d,x,y) {
	if (document.layers) {
		document.layers[d].moveTo(x,y);
	}
	else if (document.getElementById) {
		var obj=document.getElementById(d);
		obj.style.left=x+'px';
		obj.style.top=y+'px';
	}
	else if (document.all) {
		document.all[d].style.left=x+'px';
		document.all[d].style.top=y+'px';
	}
}

TermGlobals.setVisible=function(d,v) {
	if (document.layers) {
		document.layers[d].visibility= (v)? 'show':'hide';
	}
	else if (document.getElementById) {
		var obj=document.getElementById(d);
		obj.style.visibility= (v)? 'visible':'hidden';
	}
	else if (document.all) {
		document.all[d].style.visibility= (v)? 'visible':'hidden';
	}
}

TermGlobals.guiElementsReady=function(e,d) {
	if (document.layers) {
		var doc=(d)? d : self.document;
		return ((doc) && (doc.layers[e]))? true:false;
	}
	else if (document.getElementById) {
		return (document.getElementById(e))? true:false;
	}
	else if (document.all) {
		return (document.all[e])? true:false;
	}
	else return false;
}


// constructor mods (ie4 fix)

var termString_keyref;
var termString_keycoderef;

function termString_makeKeyref() {
	termString_keyref= new Array();
	termString_keycoderef= new Array();
	var hex= new Array('A','B','C','D','E','F');
	for (var i=0; i<=15; i++) {
		var high=(i<10)? i:hex[i-10];
		for (var k=0; k<=15; k++) {
			var low=(k<10)? k:hex[k-10];
			var cc=i*16+k;
			if (cc>=32) {
				var cs=unescape("%"+high+low);
				termString_keyref[cc]=cs;
				termString_keycoderef[cs]=cc;
			}
		}
	}
}

if (!String.fromCharCode) {
	termString_makeKeyref();
	String.fromCharCode=function(cc) {
		return (cc!=null)? termString_keyref[cc] : '';
	};
}
if (!String.prototype.charCodeAt) {
	if (!termString_keycoderef) termString_makeKeyref();
	String.prototype.charCodeAt=function(n) {
		cs=this.charAt(n);
		return (termString_keycoderef[cs])? termString_keycoderef[cs] : 0;
	};
}


var term ;


function termHandler() {
    var line = this.lineBuffer;
    this.newLine();
    var action = "/jcr-console/jcrconsoleservlet";
    var params = "myaction="+line+"&context="+context;
  	var response = request(action,params);
  	//Terminal.write( <text> [,<usemore>] )
    var re = /\n/g;
    var rowsCount = response.match(re).length;
    if (rowsCount > 20) {
      this.write(response,true);
      return;
    }else {
 	    this.write(response,false);
    }
    this.prompt();
}

function termOpen() {
	if (!term) {
		term = new Terminal(
			{
				x:0,
				y:0,
				rows: 20,
				cols: 175,
				greeting: '*** Welcome to eXo JCR Console ***%n',
				id: 1,
				termDiv: 'termDiv',
				crsrBlinkMode: true,
				handler: termHandler
			}
		);
		if (term) term.open();
	}
	else if (term.closed) {
		term.open();
	}
	else {
		term.focus();
	}
}

function request(url,params) {
  var req;
	if (window.XMLHttpRequest){
        req = new XMLHttpRequest();
        req.onreadystatechange = function () {};
        req.open("POST", url, false);
        req.setRequestHeader("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
        req.send(params);

    }else if (window.ActiveXObject){
    
        req = new ActiveXObject("Microsoft.XMLHTTP");
        if(req){
        req.onreadystatechange = function () {};
        req.open("POST", url, false);
        req.setRequestHeader("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
        req.send(params);
        
        }
    }
	return req.responseText;    
}
