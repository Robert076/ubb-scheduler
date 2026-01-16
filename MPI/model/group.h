#include <string>
#include <vector>

using namespace std;

class Group
{
private:
    string id; // e.g., "911"
    int size;
    string language;
    vector<string> subjects; // List of subject names they attend

    int seminarySplit;   // e.g., 1 (Whole group)
    int laboratorySplit; // e.g., 2 (Half group)

public:
    Group(string id) : id(id) {}

    void setDetails(int s, const string &lang, int semSplit, int labSplit)
    {
        size = s;
        language = lang;
        seminarySplit = semSplit;
        laboratorySplit = labSplit;
    }

    void addSubject(const string &subjectName)
    {
        subjects.push_back(subjectName);
    }

    // Getters
    string getId() const { return id; }
    int getSize() const { return size; }
    const vector<string> &getSubjects() const { return subjects; }
    int getSeminarySplit() const { return seminarySplit; }
    int getLaboratorySplit() const { return laboratorySplit; }
};