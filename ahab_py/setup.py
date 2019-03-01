import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()


setuptools.setup(
    name="ahab_py",
    version="1.0.0",
    author="RENCI",
    author_email="kthare10@renci.org",
    description="AHAB package",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/RENCI-NRIG/ahab",
    python_requires='~=3.6',
    packages=['ahab_py'],
    scripts=['bin/createSlice.py', 'bin/getVersion.py', 'bin/modifySliverSSH.py',
              'bin/sliceStatus.py', 'bin/deleteSlice.py', 'bin/listMySlices.py', 'bin/performSliceStitch.py',
              'bin/undoSliceStitch.py', 'bin/getReservationStates.py', 'bin/listResources.py',
              'bin/permitSliceStitch.py', 'bin/getReservationStitchInfo.py', 'bin/modifySlice.py',
              'bin/renewSlice.py', 'bin/getSliverProperties.py', 'bin/modifySliverGeneric.py', 'bin/revokeSliceStitch.py'],
    classifiers=[
        "Programming Language :: Python :: 3 :: Only",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
)
