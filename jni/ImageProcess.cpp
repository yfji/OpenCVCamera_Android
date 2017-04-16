#include "ImageProcess.h"

JNIEXPORT void JNICALL Java_com_example_imageprocess_ImageProcess_nativeFindRegions
  (JNIEnv * env, jobject, jlong inputAddr, jlong outputAddr, jobject regions){
	Mat input= *((Mat*)inputAddr);
	Mat output= *((Mat*)outputAddr);
	int thresLow = 100;
	int kernelSize = 3;
	int t_color=255;
	Mat gSource=input, gaussian, contour;
	if(input.channels()==3)
		cvtColor(input, gSource, CV_BGR2GRAY);
	GaussianBlur(gSource, gaussian, Size(kernelSize, kernelSize), 0.1);
	Canny(gaussian, contour, thresLow, thresLow*3);
	int w = contour.cols, h = contour.rows;
	char* used = new char[w*h];
	int* _stack = new int[w*h];
	int* prevv = new int[w*h];
	int* region = new int[w*h];
	uchar* data = contour.data;
	memset(used, 0, w*h*sizeof(char));

	int cnt = -1, prev_cnt = 0, t_max = 0, start = 0;
	for (int k = 0; k < w*h;++k){
		if (used[k] || data[k] != t_color)
			continue;
		int top = -1;
		char pushed = 0;
		_stack[++top] = k;
		region[++cnt] = k;
		used[k] = 1;
		while (top >= 0) {
			pushed = 0;
			int s = _stack[top];
			if (s >= w && !used[s - w] && data[s - w] == t_color) {	//up
				prevv[s - w] = s;
				_stack[++top] = s - w;
				used[s - w] = 1;
				pushed = 1;
				region[++cnt] = s - w;
			}
			else if (s < (h - 1)*w && !used[s + w] && data[s + w] == t_color) {	//down
				prevv[s + w] = s;
				_stack[++top] = s + w;
				used[s + w] = 1;
				pushed = 1;
				region[++cnt] = s + w;
			}
			else if (s%w != 0 && !used[s - 1] && data[s - 1] == t_color) {		//left
				_stack[++top] = s - 1;
				used[s - 1] = 1;
				pushed = 1;
				region[++cnt] = s - 1;
			}
			else if (s % (w - 1) != 0 && !used[s + 1] && data[s + 1] == t_color) {		//right
				_stack[++top] = s + 1;
				used[s + 1] = 1;
				pushed = 1;
				region[++cnt] = s + 1;
			}
			else if (s >= w && s%w != 0 && !used[s - w - 1] && data[s - w - 1] == t_color) {
				_stack[++top] = s - w - 1;
				used[s - w - 1] = 1;
				pushed = 1;
				region[++cnt] = s - w -1 ;
			}
			else if (s >= w && s % (w - 1) != 0 && !used[s - w + 1] && data[s - w + 1] == t_color) {
				_stack[++top] = s - w + 1;
				used[s - w + 1] = 1;
				pushed = 1;
				region[++cnt] = s - w + 1 ;
			}
			else if (s < (h - 1)*w && s%w != 0 && !used[s + w - 1] && data[s + w - 1] == t_color) {
				_stack[++top] = s + w - 1;
				used[s + w - 1] = 1;
				pushed = 1;
				region[++cnt] = s + w - 1 ;
			}
			else if (s < (h - 1)*w && s % (w - 1) != 0 && !used[s + w + 1] && data[s + w + 1] == t_color) {
				_stack[++top] = s + w + 1;
				used[s + w + 1] = 1;
				pushed = 1;
				region[++cnt] = s + w + 1;
			}
			if (!pushed) {
				--top;
			}
		}
		if (cnt - prev_cnt>t_max) {
			t_max = cnt - prev_cnt;
			start = prev_cnt;
		}
		prev_cnt = cnt;
	}
	int top_x = 0x7ffffff, top_y = top_x, bottom_x = -1, bottom_y = -1;
	for (int i = start; i < t_max; ++i) {
		int s = region[i];
		int x = s%w, y = s / w;
		if (top_x > x)	top_x = x;
		if (top_y > y)	top_y = y;
		if (bottom_x < x)	bottom_x = x;
		if (bottom_y < y)	bottom_y = y;
	}
	rectangle(output, Rect(top_x, top_y, bottom_x-top_x, bottom_y-top_y), Scalar(255,0,0), 1);
	delete used, _stack, prevv, region;
}


