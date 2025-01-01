# SimbaDroid

![App logo](app/src/main/res/mipmap-mdpi/ic_launcher.png)

SimbaDroid is an SMB server for Android, allowing you to access the contents of your storage (both
internal storage or an external SD card) from your computer via wi-fi.

## Download

<a href="https://f-droid.org/packages/de.buttercookie.simbadroid">
<img src="https://f-droid.org/badge/get-it-on.png" width="230">
</a><br>
&nbsp;&nbsp;&nbsp; <a href="https://apt.izzysoft.de/fdroid/index/apk/de.buttercookie.simbadroid">
<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButton.png" width="200">
</a>

… or download it directly from the [Github Releases page](https://github.com/buttercookie42/SimbaDroid/releases).

## Notes

Due to Android limitations, this app requires root (as on all Unixoid operating systems, it is not
possible to open a port < 1024 otherwise, and the default SMB port is 445 and Windows doesn't work
with anything else). Without root, you either need some sort of SMB client which allows configuring
the port used (SimbaDroid uses port 4450 behind the scenes), or some kind of port mapping software
to allow Windows to transparently connect to port 4450.

Due to [JFileServer](https://github.com/FileSysOrg/jfileserver) limitations, only SMBv1 is
supported, which isn't ideal for modern Windows, either, but c'est la vie…

Since the app is mostly for my private use, at the moment there are no configuration options –
shares for the internal storage as well as an external SD card (if one is detected) are set up
automatically and are accessible without authentication, so be careful about using this app in
public networks. I might add some options for configuring your own custom shares and user
authentication eventually, but no guarantees as to if and when.

`READ/WRITE/MANAGE_EXTERNAL_STORAGE` is required for filesystem access.

## Compatibility

SimbaDroid should run on Android 6 and newer, though only Android 6 and 14 have been tested so far.

## Licenses

SimbaDroid is © Jan Henning, 2024 and is mostly provided under the Mozilla Public License 2.0
(see LICENSE.md)

Portions of the code are provided under the Apache License, Version 2.0 (see apache-2.0.txt)

"[Lion](https://thenounproject.com/icon/lion-6029941/)" icon used in the logo by Gabriel Baudon
from [the Noun Project](https://thenounproject.com/)  under the
[CC BY 3.0 license](https://creativecommons.org/licenses/by/3.0/).

_JFileServer_ is used under the LGPL-3.0 license (see lgpl-3.0.md and gpl-3.0.md). Currently,
SimbaDroid utilises a [forked version](https://github.com/buttercookie42/jfileserver) with some
additional fixes.

_libsu_, _JmDNS_, _Apache Commons Lang_, _Guava_ and the _Android Support Library_ are used under
the Apache License, Version 2.0

_SLF4J_ is used under the **MIT license**:

Copyright (c) 2004-2022 QOS.ch Sarl (Switzerland)
All rights reserved.

Permission is hereby granted, free  of charge, to any person obtaining
a  copy  of this  software  and  associated  documentation files  (the
"Software"), to  deal in  the Software without  restriction, including
without limitation  the rights to  use, copy, modify,  merge, publish,
distribute,  sublicense, and/or sell  copies of  the Software,  and to
permit persons to whom the Software  is furnished to do so, subject to
the following conditions:

The  above  copyright  notice  and  this permission  notice  shall  be
included in all copies or substantial portions of the Software.

THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

_Bouncy Castle_ is used under the following license:

**Bouncy Castle License**

Copyright (c) 2000 - 2024 The Legion of the Bouncy Castle Inc. (https://www.bouncycastle.org)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

### Notices
#### JmDNS

Java Multicast Domain Name Server (JmDNS)

This project was originally developed by Arthur van Hoff under the GNU
Lesser General Public License as jRendevous.  It was moved to Sourceforge
by Rick Blair and renamed to JmDNS with the Arthur's kind permission.
It has been re-released under the Apache License, Version 2.0.
In 2014, it has been moved from Sourceforge to Github by Kai Kreuzer
with the kind approval from Arthur and Rick.

Details of the Apache License, Version 2.0 can be found at:
http://www.apache.org/licenses/

#### Apache Commons Lang

Apache Commons Lang

Copyright 2001-2024 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).