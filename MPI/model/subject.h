#include <string>

using namespace std;

class Subject
{
private:
    string name;
    string mainTeacher;
    string language;

    // Frequencies (double to handle 0.5 cases like bi-weekly labs)
    double coursesPerWeek;
    double seminarsPerWeek;
    double laboratoriesPerWeek;

    // Durations (in hours)
    int courseLength;
    int seminarLength;
    int laboratoryLength;

public:
    Subject(string name) : name(name) {}

    // Getters
    string getName() const { return name; }
    string getMainTeacher() const { return mainTeacher; }
    string getLanguage() const { return language; }

    double getCoursesPerWeek() const { return coursesPerWeek; }
    int getCourseLength() const { return courseLength; }

    double getSeminarsPerWeek() const { return seminarsPerWeek; }
    int getSeminarLength() const { return seminarLength; }

    double getLaboratoriesPerWeek() const { return laboratoriesPerWeek; }
    int getLaboratoryLength() const { return laboratoryLength; }

    // Setters
    void setDetails(const string &teacher, const string &lang)
    {
        mainTeacher = teacher;
        language = lang;
    }

    void setCourseConfig(double count, int length)
    {
        coursesPerWeek = count;
        courseLength = length;
    }

    void setSeminarConfig(double count, int length)
    {
        seminarsPerWeek = count;
        seminarLength = length;
    }

    void setLaboratoryConfig(double count, int length)
    {
        laboratoriesPerWeek = count;
        laboratoryLength = length;
    }
};